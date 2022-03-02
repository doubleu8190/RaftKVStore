package cn.ttplatform.wh.log4j;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.MDC;

/**
 * @author Wang Hao
 * @date 2022/2/10 15:46
 */
@Sharable
public class Log4jContextSetupHandler extends ChannelInboundHandlerAdapter {

    private static final String CHANNEL = "channel";

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        MDC.put(CHANNEL, channel.toString());
        super.channelActive(ctx);
    }

}
