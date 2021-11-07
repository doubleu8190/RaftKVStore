package cn.ttplatform.wh.support;

import cn.ttplatform.wh.cmd.RequestFailedCommand;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Wang Hao
 * @date 2021/4/28 23:40
 */
@Slf4j
public class DistributableCodec extends ByteToMessageCodec<Distributable> {

    private static final int FIXED_MESSAGE_HEADER_LENGTH = Integer.BYTES + Integer.BYTES;

    private final DistributableSerializerRegistry serializerRegistry;
    private RequestFailedCommand requestFailedCommand;

    public DistributableCodec(DistributableSerializerRegistry serializerRegistry) {
        this.serializerRegistry = serializerRegistry;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Distributable msg, ByteBuf out) {
        DistributableSerializer serializer = serializerRegistry.getSerializer(msg.getType());
        serializer.serialize(msg, out);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < FIXED_MESSAGE_HEADER_LENGTH) {
            return;
        }
        // 4(type) + 4(contentLength) + byte[contentLength]
        in.markReaderIndex();
        int type = in.readInt();
        int contentLength = in.readInt();
        int newReaderIndex = in.readerIndex() + contentLength;
        if (in.readableBytes() < contentLength) {
            in.resetReaderIndex();
            return;
        }
        try {
            DistributableSerializer serializer = serializerRegistry.getSerializer(type);
            Distributable distributable = serializer.deserialize(in.nioBuffer(), contentLength);
            out.add(distributable);
        } catch (Exception e) {
            log.error(e.getMessage());
            ctx.channel().writeAndFlush(failedCommand(e.getMessage()));
        } finally {
            in.readerIndex(newReaderIndex);
        }
    }

    private RequestFailedCommand failedCommand(String failed) {
        if (requestFailedCommand == null) {
            this.requestFailedCommand = new RequestFailedCommand(failed);
        } else {
            requestFailedCommand.setFailedMessage(failed);
        }
        return requestFailedCommand;
    }
}
