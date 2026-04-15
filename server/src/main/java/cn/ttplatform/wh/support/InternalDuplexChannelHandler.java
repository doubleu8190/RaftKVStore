package cn.ttplatform.wh.support;

import cn.ttplatform.wh.GlobalContext;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

/**
 * @author : wang hao
 * @date :  2020/8/16 0:19
 **/
@Slf4j
@Sharable
public class InternalDuplexChannelHandler extends AbstractDuplexChannelHandler {

    public InternalDuplexChannelHandler(GlobalContext context) {
        super(context);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        Channel channel = ctx.channel();
        if (msg instanceof Message) {
            String sourceId = ((Message) msg).getSourceId();
            log.debug("receive a msg {} from {}.", msg, sourceId);
            channelPool.cacheChannel(sourceId, channel);
            distributor.distribute((Message) msg);
            ctx.fireChannelRead(msg);
        } else {
            log.error("unknown message type, msg is {}", msg);
            channel.close();
        }
    }
}