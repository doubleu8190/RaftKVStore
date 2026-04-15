package cn.ttplatform.wh.group;

import cn.ttplatform.wh.GlobalContext;
import cn.ttplatform.wh.support.DefaultMessageCodec;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;

/**
 * @author : Wang Hao
 * @date :  2020/8/16 18:22
 **/
@Sharable
public class CoreChannelInitializer extends ChannelInitializer<SocketChannel> {

    private final GlobalContext context;

    public CoreChannelInitializer(GlobalContext context) {
        this.context = context;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new DefaultMessageCodec(context.getSerializerRegistry()));
        pipeline.addLast(new CoreDuplexChannelHandler(context));
    }
}
