package cn.ttplatform.wh.support;

import cn.ttplatform.wh.cmd.factory.ClusterChangeCommandSerializer;
import cn.ttplatform.wh.cmd.factory.ClusterChangeResultCommandSerializer;
import cn.ttplatform.wh.cmd.factory.GetClusterInfoCommandSerializer;
import cn.ttplatform.wh.cmd.factory.GetClusterInfoResultCommandSerializer;
import cn.ttplatform.wh.cmd.factory.GetCommandSerializer;
import cn.ttplatform.wh.cmd.factory.GetResultCommandSerializer;
import cn.ttplatform.wh.cmd.factory.RedirectCommandSerializer;
import cn.ttplatform.wh.cmd.factory.RequestFailedCommandSerializer;
import cn.ttplatform.wh.cmd.factory.SetCommandSerializer;
import cn.ttplatform.wh.cmd.factory.SetResultCommandSerializer;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.SocketChannel;
import io.protostuff.LinkedBuffer;

/**
 * Sets up the Netty pipeline for the client channel.
 *
 * @author Wang Hao
 * @date 2021/5/26 21:25
 */
public class ClientChannelInitializer extends ChannelInitializer<SocketChannel> {

    private static final int LINKED_BUFFER_POOL_SIZE = 16;

    private final ClientDuplexChannelHandler channelHandler;
    private final DistributableCodec codec;

    public ClientChannelInitializer(ClientDuplexChannelHandler channelHandler) {
        this.channelHandler = channelHandler;
        // Build serializer registry with all 10 client-facing command serializers
        Pool<LinkedBuffer> bufferPool = new FixedSizeLinkedBufferPool(LINKED_BUFFER_POOL_SIZE);
        DistributableSerializerRegistry registry = new DistributableSerializerRegistry();
        registry.register(new SetCommandSerializer(bufferPool));
        registry.register(new SetResultCommandSerializer(bufferPool));
        registry.register(new GetCommandSerializer(bufferPool));
        registry.register(new GetResultCommandSerializer(bufferPool));
        registry.register(new RedirectCommandSerializer(bufferPool));
        registry.register(new ClusterChangeCommandSerializer(bufferPool));
        registry.register(new ClusterChangeResultCommandSerializer(bufferPool));
        registry.register(new RequestFailedCommandSerializer(bufferPool));
        registry.register(new GetClusterInfoCommandSerializer(bufferPool));
        registry.register(new GetClusterInfoResultCommandSerializer(bufferPool));
        this.codec = new DistributableCodec(registry);
    }

    @Override
    protected void initChannel(SocketChannel ch) {
        ch.pipeline()
            .addLast("codec", codec)
            .addLast("handler", channelHandler);
    }
}
