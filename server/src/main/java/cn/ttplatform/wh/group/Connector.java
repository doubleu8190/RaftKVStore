package cn.ttplatform.wh.group;

import cn.ttplatform.wh.GlobalContext;
import cn.ttplatform.wh.config.ServerProperties;
import cn.ttplatform.wh.support.ChannelPool;
import cn.ttplatform.wh.support.Message;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.net.InetSocketAddress;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Wang Hao
 * @date 2021/7/12 12:59
 */
@Slf4j
@Builder
@AllArgsConstructor
public class Connector {

    private final Bootstrap bootstrap;
    private final GlobalContext context;
    private final ChannelPool channelPool;
    private final NioEventLoopGroup boss;
    private final NioEventLoopGroup worker;

    public Connector(GlobalContext context) {
        this.context = context;
        this.channelPool = context.getChannelPool();
        this.boss = context.getBoss();
        this.worker = context.getWorker();
        this.bootstrap = newBootstrap(context.getWorker());
    }

    public Bootstrap newBootstrap(EventLoopGroup worker) {
        return new Bootstrap().group(worker)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                .handler(new CoreChannelInitializer(context));
    }

    public void listen(String host, int port) {
        ServerBootstrap serverBootstrap = new ServerBootstrap().group(boss, worker)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                .childHandler(new CoreChannelInitializer(context));
        try {
            serverBootstrap.bind(host, port).addListener(future -> {
                if (future.isSuccess()) {
                    log.info("Connector start in {}:{}", host, port);
                }
            }).sync();
        } catch (InterruptedException e) {
            log.error("failed to start Connector.");
            Thread.currentThread().interrupt();
        }
    }

    public Channel getConnection(InetSocketAddress socketAddress, String remoteId, boolean channelCached) {
        Channel channel;
        if (channelCached) {
            channel = channelPool.getChannel(remoteId);
            if (channel != null) {
                if (!channel.isOpen()) {
                    // channel is closed, must be removed
                    channelPool.removeChannel(remoteId);
                }
                return channel;
            }
        }
        channel = connect(socketAddress);
        if (channelCached) {
            channelPool.cacheChannel(remoteId, channel);
            channel.closeFuture().addListener(future -> {
                if (future.isSuccess()) {
                    channelPool.removeChannel(remoteId);
                }
            });
        }
        return channel;
    }

    public Channel connect(InetSocketAddress socketAddress) {
        try {
            return bootstrap.connect(socketAddress).sync().channel();
        } catch (Exception e) {
            log.error("failed to connect to {}.", socketAddress);
            Thread.currentThread().interrupt();
            return null;
        }
    }

    public ChannelFuture send(Message message, EndpointMetaData metaData, boolean cmd) {
        Channel channel;
        if (cmd) {
            channel = getConnection(metaData.getCommandAddress(), null, false);
        } else {
            channel = getConnection(metaData.getConnectorAddress(), metaData.getNodeId(), true);
        }
        if (channel == null) {
            return null;
        }
        log.debug("encode a message[{}] to {}", message, metaData.getNodeId());
        return channel.writeAndFlush(message);
    }
}
