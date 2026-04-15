package cn.ttplatform.wh.support;

import cn.ttplatform.wh.GlobalContext;
import cn.ttplatform.wh.config.ServerProperties;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;

/**
 * @author : Wang Hao
 * @date :  2020/8/16 18:22
 **/
@Sharable
public class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    protected final GlobalContext context;

    public ServerChannelInitializer(GlobalContext context) {
        this.context = context;
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ServerProperties serverProperties = context.getProperties();
        ChannelPipeline pipeline = ch.pipeline();
        int writeBufferHighWaterMark = serverProperties.getWriteBufferHighWaterMark();
        ch.config().setWriteBufferHighWaterMark(writeBufferHighWaterMark);
        long readLimit = serverProperties.getChannelTrafficShapingHandlerReadLimit();
        long writeLimit = serverProperties.getChannelTrafficShapingHandlerWriteLimit();
        pipeline.addLast(new ChannelTrafficShapingHandler(0, readLimit, writeLimit));
        int readIdleTimeout = serverProperties.getReadIdleTimeout();
        int writeIdleTimeout = serverProperties.getWriteIdleTimeout();
        int allIdleTimeout = serverProperties.getAllIdleTimeout();
        pipeline.addLast(new IdleStateHandler(readIdleTimeout, writeIdleTimeout, allIdleTimeout));
        short protocolMagicNumber = serverProperties.getProtocolMagicNumber();
        pipeline.addLast(new DefaultCommandCodec(context.getSerializerRegistry(), protocolMagicNumber));
        pipeline.addLast(new ServerDuplexChannelHandler(context));
    }
}
