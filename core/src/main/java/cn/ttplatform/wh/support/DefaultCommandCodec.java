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
public class DefaultCommandCodec extends ByteToMessageCodec<Distributable> {

    private static final byte VERSION = 1;
    private static final int HEADER_LENGTH = 2 + 1 + 1 + 4;

    private final DistributableSerializerRegistry serializerRegistry;
    private final short magicNumber;
    private RequestFailedCommand requestFailedCommand;

    public DefaultCommandCodec(DistributableSerializerRegistry serializerRegistry) {
        this(serializerRegistry, (short) 0x5A5A);
    }

    public DefaultCommandCodec(DistributableSerializerRegistry serializerRegistry, short magicNumber) {
        this.serializerRegistry = serializerRegistry;
        this.magicNumber = magicNumber;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Distributable msg, ByteBuf out) {
        DistributableSerializer serializer = serializerRegistry.getSerializer(msg.getType());

        // 保存当前位置用于后续更新长度字段
        int writerIndex = out.writerIndex();

        // 写入header
        out.writeShort(magicNumber);
        out.writeByte(VERSION);
        out.writeByte((byte) msg.getType()); // 消息类型，确保在byte范围内
        out.writeInt(0); // 数据长度占位，稍后更新

        // 记录消息体开始位置
        int bodyStartIndex = out.writerIndex();

        // 序列化消息体
        serializer.serialize(msg, out);

        // 计算消息体长度
        int bodyLength = out.writerIndex() - bodyStartIndex;

        // 更新数据长度字段
        out.setInt(writerIndex + 2 + 1 + 1, bodyLength);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        if (in.readableBytes() < HEADER_LENGTH) {
            return;
        }

        in.markReaderIndex();

        // 读取并验证魔数
        short magic = in.readShort();
        if (magic != magicNumber) {
            in.resetReaderIndex();
            log.error("Illegal connection detected, invalid magic number: 0x{}, expected: 0x{}, closing connection",
                    String.format("%04X", magic & 0xFFFF), String.format("%04X", magicNumber & 0xFFFF));
            ctx.channel().close(); // 关闭非法连接
            return;
        }

        // 读取header其他字段
        byte version = in.readByte();
        // 验证版本号
        if (version != VERSION) {
            in.resetReaderIndex();
            log.error("Unsupported protocol version: {}, expected: {}", version, VERSION);
            ctx.channel().close(); // 关闭非法连接
            return;
        }
        byte type = in.readByte();
        int contentLength = in.readInt();

        // 检查是否有足够的数据
        if (in.readableBytes() < contentLength) {
            in.resetReaderIndex();
            return;
        }

        int newReaderIndex = in.readerIndex() + contentLength;

        try {
            // 将byte类型转换为int（DistributableType使用int）
            int typeInt = type & 0xFF;
            DistributableSerializer serializer = serializerRegistry.getSerializer(typeInt);
            Distributable distributable = serializer.deserialize(in.nioBuffer(), contentLength);

            log.debug("Received message type: {}", typeInt);

            out.add(distributable);
        } catch (Exception e) {
            log.error("Failed to decode message: {}", e.getMessage());
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
