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
 * @date 2021/4/28 10:28
 */
class OldNewConfigSerializer implements Serializer<OldNewConfig> {

    private final Pool<LinkedBuffer> pool;
    private final Schema<OldNewConfig> schema = RuntimeSchema.getSchema(OldNewConfig.class);

    public OldNewConfigSerializer(Pool<LinkedBuffer> pool) {
        this.pool = pool;
    }

    @Override
    public OldNewConfig deserialize(byte[] content, int length) {
        OldNewConfig oldNewConfig = new OldNewConfig();
        ProtostuffIOUtil.mergeFrom(content, 0, length, oldNewConfig, schema);
        return oldNewConfig;
    }

    @Override
    public OldNewConfig deserialize(ByteBuffer byteBuffer, int contentLength) {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] serialize(OldNewConfig oldNewConfig) {
        return ProtostuffIOUtil.toByteArray(oldNewConfig, schema, pool.allocate());
    }

    @Override
    public void serialize(OldNewConfig obj, ByteBuf byteBuffer) {
        throw new UnsupportedOperationException();
    }
}
