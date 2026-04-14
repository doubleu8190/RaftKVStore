package cn.ttplatform.wh.support;

import cn.ttplatform.wh.GlobalContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;

/**
 * @author : Wang Hao
 * @date :  2020/8/16 18:22
 **/
@Sharable
public class ServerChannelInitializer extends AbstractChannelInitializer {

    private final ServerDuplexChannelHandler serverDuplexChannelHandler;
    private final ChannelTrafficShapingHandler channelTrafficShapingHandler;

    public ServerChannelInitializer(GlobalContext context) {
        super(context);
        this.serverDuplexChannelHandler = new ServerDuplexChannelHandler(context);
        long readLimit = context.getProperties().getChannelTrafficShapingHandlerReadLimit();
        long writeLimit = context.getProperties().getChannelTrafficShapingHandlerWriteLimit();
        this.channelTrafficShapingHandler = new ChannelTrafficShapingHandler(0, readLimit, writeLimit);
    }

    @Override
    protected void custom(ChannelPipeline pipeline) {
        pipeline.addLast(channelTrafficShapingHandler);
        pipeline.addLast(serverDuplexChannelHandler);
    }
}
