package cn.ttplatform.wh.support;

import cn.ttplatform.wh.cmd.RequestFailedCommand;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class DefaultMessageCodec extends ByteToMessageCodec<Distributable>{
    private static final int FIXED_MESSAGE_HEADER_LENGTH = Byte.BYTES + Integer.BYTES;

    private final DistributableSerializerRegistry serializerRegistry;
    private RequestFailedCommand requestFailedCommand;

    public DefaultMessageCodec(DistributableSerializerRegistry serializerRegistry) {
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
        // 1(type) + 4(contentLength) + byte[contentLength]
        in.markReaderIndex();
        byte type = in.readByte();
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
