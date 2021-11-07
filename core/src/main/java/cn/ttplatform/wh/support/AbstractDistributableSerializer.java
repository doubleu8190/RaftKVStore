package cn.ttplatform.wh.support;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.protostuff.LinkedBuffer;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * @author Wang Hao
 * @date 2021/3/15 14:25
 */
public abstract class AbstractDistributableSerializer implements DistributableSerializer {

    Pool<LinkedBuffer> pool;

    protected AbstractDistributableSerializer(Pool<LinkedBuffer> pool) {
        this.pool = pool;
    }

    @Override
    public byte[] serialize(Distributable distributable) {
        LinkedBuffer buffer = pool.allocate();
        try {
            return serialize(distributable, buffer);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            pool.recycle(buffer);
        }
    }

    /**
     * serialize a message by protostuff, then write the buffer content into a byte array.
     *
     * @param distributable source message
     * @param buffer        dst LinkedBuffer
     * @return byte array
     */
    public abstract byte[] serialize(Distributable distributable, LinkedBuffer buffer);

    @Override
    public void serialize(Distributable distributable, ByteBuf byteBuffer) {
        // write the message type(4 bytes) into buffer
        byteBuffer.writeInt(getFactoryType());
        int writerIndex = byteBuffer.writerIndex();
        byteBuffer.writeInt(getFactoryType());
        ByteBufOutputStream byteBufOutputStream = new ByteBufOutputStream(byteBuffer);
        LinkedBuffer buffer = pool.allocate();
        try {
            serialize(distributable, buffer, byteBufOutputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            pool.recycle(buffer);
        }
        int newWriterIndex = byteBuffer.writerIndex();
        // back off the writerIndex(offset=4), then record the contentLength(4 bytes)
        byteBuffer.writerIndex(writerIndex);
        byteBuffer.writeInt(newWriterIndex - 8);
        // restore the writerIndex
        byteBuffer.writerIndex(newWriterIndex);
    }

    /**
     * serialize a message by protostuff, then write the buffer content into outputStream.
     *
     * @param distributable source message
     * @param buffer        dst LinkedBuffer
     * @param outputStream  dst OutputStream
     * @throws IOException a parse exception
     */
    public abstract void serialize(Distributable distributable, LinkedBuffer buffer, OutputStream outputStream) throws IOException;


    @Override
    public Distributable deserialize(ByteBuffer byteBuffer, int contentLength) {
        int limit = byteBuffer.limit();
        try {
            int position = byteBuffer.position();
            byteBuffer.limit(position + contentLength);
            return create(byteBuffer);
        } finally {
            byteBuffer.limit(limit);
        }
    }

    /**
     * deserialize a obj from ByteBuffer
     *
     * @param byteBuffer source
     * @return a Distributable obj
     */
    public abstract Distributable create(ByteBuffer byteBuffer);
}
