package cn.ttplatform.wh.support;

import cn.ttplatform.wh.GlobalContext;
import cn.ttplatform.wh.Node;
import cn.ttplatform.wh.cmd.Command;
import cn.ttplatform.wh.cmd.RedirectCommand;
import cn.ttplatform.wh.config.ServerProperties;
import cn.ttplatform.wh.role.Follower;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author : wang hao
 * @date : 2020/8/16 0:19
 **/
@Slf4j
@Sharable
public class ServerDuplexChannelHandler extends AbstractDuplexChannelHandler {

    private final Map<Channel, LazyFlushStrategy> channelFlushStrategyMap;
    private final Map<Channel, Queue<Object>> channelMessageQueues;
    private final int queueCapacity;

    public ServerDuplexChannelHandler(GlobalContext context) {
        super(context);
        ServerProperties properties = context.getProperties();
        if (properties.isTcpNoDelay()) {
            channelFlushStrategyMap = null;
        } else {
            log.debug("enable lazy flush strategy.");
            channelFlushStrategyMap = new ConcurrentHashMap<>((int) (properties.getBlockSize() / 0.75f) + 1);
        }
        channelMessageQueues = new ConcurrentHashMap<>((int) (properties.getBlockSize() / 0.75f) + 1);
        queueCapacity = properties.getQueueCapacity();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        log.debug("active a channel[{}]", channel);
        ServerProperties properties = context.getProperties();
        if (channelFlushStrategyMap != null) {
            channelFlushStrategyMap.put(channel, new LazyFlushStrategy(channel, properties.getLazyFlushInterval(),
                    properties.getLazyFlushThreshold()));
        }
        channelMessageQueues.put(channel, new LinkedBlockingQueue<>(queueCapacity));
    }

    private boolean sendQueuedMessages(ChannelHandlerContext ctx, Channel channel) {
        Queue<Object> queue = channelMessageQueues.get(channel);
        if (queue == null || queue.isEmpty()) {
            return false;
        }

        int sent = 0;
        while (channel.isWritable() && !queue.isEmpty()) {
            Object msg = queue.poll();
            if (msg != null) {
                ctx.write(msg);
                sent++;
            }
        }

        if (sent > 0) {
            ctx.flush();
            log.debug("Sent {} queued messages for channel {}, remaining queue size: {}",
                    sent, channel, queue.size());
        }

        return !queue.isEmpty();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Channel channel = ctx.channel();
        if (msg instanceof Command) {
            Command command = (Command) msg;
            String commandId = command.getId();
            if (!canHandler(command, ctx)) {
                return;
            }
            channelPool.cacheChannel(commandId, channel);
            distributor.distribute(command);
            ctx.fireChannelRead(msg);
        } else {
            log.error("unknown message type, msg is {} from {}", msg, channel);
            channel.close();
        }
    }

    private boolean canHandler(Command command, ChannelHandlerContext ctx) {
        Node node = context.getNode();
        if (!node.isLeader()) {
            String leaderId = null;
            if (node.isFollower()) {
                Follower role = (Follower) context.getNode().getRole();
                leaderId = role.getLeaderId();
            }
            log.info("current role is not a leader, redirect request to node[id={}]", leaderId);
            ctx.channel().writeAndFlush(RedirectCommand.builder().id(command.getId()).leader(leaderId)
                    .endpointMetaData(context.getCluster().getAllEndpointMetaData().toString()).build());
            return false;
        }
        return true;
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        if (channelFlushStrategyMap != null) {
            LazyFlushStrategy lazyFlushStrategy = channelFlushStrategyMap.get(ctx.channel());
            if (lazyFlushStrategy != null && lazyFlushStrategy.flush()) {
                super.flush(ctx);
            }
        } else {
            super.flush(ctx);
        }
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        Channel channel = ctx.channel();
        if (channelFlushStrategyMap != null) {
            channelFlushStrategyMap.remove(channel);
        }
        if (channelMessageQueues != null) {
            Queue<Object> queue = channelMessageQueues.remove(channel);
            if (queue != null && !queue.isEmpty()) {
                log.warn("Channel {} closed with {} queued messages in memory, messages will be lost", channel,
                        queue.size());
            }
        }
        super.close(ctx, promise);
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        if (!channel.isWritable()) {
            log.warn("Channel {} is not writable, high water mark reached", channel);
        } else {
            log.debug("Channel {} is writable again", channel);

            // 发送内存队列中的消息
            sendQueuedMessages(ctx, channel);
        }
        super.channelWritabilityChanged(ctx);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        Channel channel = ctx.channel();

        // 如果通道可写，先尝试发送积压的消息
        if (channel.isWritable()) {
            // 1. 先发送内存队列中的消息
            sendQueuedMessages(ctx, channel);

            // 2. 发送当前消息
            if (channel.isWritable()) {
                super.write(ctx, msg, promise.addListener(future -> {
                    if (!future.isSuccess()) {
                        log.error("Write failed for channel {}, cause: {}", channel, future.cause());
                    }
                }));
                return;
            }
            // 如果发送积压消息后通道变得不可写，继续下面的逻辑
        }

        // 通道不可写，处理背压
        log.warn("Channel {} is not writable, handling backpressure", channel);

        // 尝试放入内存队列
        Queue<Object> queue = channelMessageQueues.get(channel);
        if (queue != null) {
            if (queue.size() < queueCapacity) {
                boolean offered = queue.offer(msg);
                if (offered) {
                    log.debug("Message queued in memory for channel {}, queue size: {}", channel, queue.size());
                    promise.setSuccess();
                    return;
                }
            }

            // 内存队列已满，丢弃消息并报错
            log.error("Memory queue full for channel {}, dropping message", channel);
            promise.setFailure(new Exception("Backpressure handling failed: memory queue is full"));
            return;
        }

        // 如果没有队列（不应该发生），直接发送
        super.write(ctx, msg, promise.addListener(future -> {
            if (!future.isSuccess()) {
                log.error("Write failed for channel {}, cause: {}", channel, future.cause());
            }
        }));
    }

}