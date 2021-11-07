package cn.ttplatform.wh.group;

import cn.ttplatform.wh.support.Pool;
import cn.ttplatform.wh.support.Serializer;
import io.netty.buffer.ByteBuf;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import java.nio.ByteBuffer;

/**
 * @author Wang Hao
 * @date 2021/4/28 10:29
 */
public class NewConfigSerializer implements Serializer<NewConfig> {

    private final Pool<LinkedBuffer> pool;
    private final Schema<NewConfig> schema = RuntimeSchema.getSchema(NewConfig.class);

    public NewConfigSerializer(Pool<LinkedBuffer> pool) {
        this.pool = pool;
    }

    @Override
    public NewConfig deserialize(byte[] content, int length) {
        NewConfig newConfig = new NewConfig();
        ProtostuffIOUtil.mergeFrom(content, 0, length, newConfig, schema);
        return newConfig;
    }

    @Override
    public NewConfig deserialize(ByteBuffer byteBuffer, int contentLength) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] serialize(NewConfig newConfig) {
        return ProtostuffIOUtil.toByteArray(newConfig, schema, pool.allocate());
    }

    @Override
    public void serialize(NewConfig obj, ByteBuf byteBuffer) {
        throw new UnsupportedOperationException();
    }
}
