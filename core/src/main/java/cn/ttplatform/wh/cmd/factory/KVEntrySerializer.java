package cn.ttplatform.wh.cmd.factory;

import cn.ttplatform.wh.cmd.KeyValuePair;
import cn.ttplatform.wh.support.Serializer;
import cn.ttplatform.wh.support.Pool;
import io.netty.buffer.ByteBuf;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import java.nio.ByteBuffer;

/**
 * @author Wang Hao
 * @date 2021/5/19 21:54
 */
public class KVEntrySerializer implements Serializer<KeyValuePair> {


    private final Schema<KeyValuePair> schema = RuntimeSchema.getSchema(KeyValuePair.class);
    private final Pool<LinkedBuffer> pool;

    public KVEntrySerializer(Pool<LinkedBuffer> pool) {
        this.pool = pool;
    }

    @Override
    public KeyValuePair deserialize(byte[] content, int contentLength) {
        KeyValuePair keyValuePair = new KeyValuePair();
        ProtostuffIOUtil.mergeFrom(content, 0, contentLength, keyValuePair, schema);
        return keyValuePair;
    }

    @Override
    public KeyValuePair deserialize(ByteBuffer byteBuffer, int contentLength) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] serialize(KeyValuePair obj) {
        LinkedBuffer buffer = pool.allocate();
        try {
            return ProtostuffIOUtil.toByteArray(obj, schema, buffer);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        } finally {
            pool.recycle(buffer);
        }
    }

    @Override
    public void serialize(KeyValuePair obj, ByteBuf byteBuffer) {
        throw new UnsupportedOperationException();
    }
}
