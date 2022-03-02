package cn.ttplatform.wh.support;

import cn.ttplatform.wh.GlobalContext;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelPipeline;

/**
 * @author : Wang Hao
 * @date :  2020/8/16 18:22
 **/
@Sharable
public class ServerChannelInitializer extends AbstractChannelInitializer {

    private final ServerDuplexChannelHandler serverDuplexChannelHandler;

    public ServerChannelInitializer(GlobalContext context) {
        super(context);
        this.serverDuplexChannelHandler = new ServerDuplexChannelHandler(context);
    }

    @Override
    protected void custom(ChannelPipeline pipeline) {
        pipeline.addLast(serverDuplexChannelHandler);
    }
}
