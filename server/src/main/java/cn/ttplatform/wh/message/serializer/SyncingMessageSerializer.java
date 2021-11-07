package cn.ttplatform.wh.message.serializer;

import cn.ttplatform.wh.message.SyncingMessage;
import cn.ttplatform.wh.constant.DistributableType;
import cn.ttplatform.wh.constant.ErrorMessage;
import cn.ttplatform.wh.exception.MessageParseException;
import cn.ttplatform.wh.support.AbstractDistributableSerializer;
import cn.ttplatform.wh.support.Distributable;
import cn.ttplatform.wh.support.Pool;
import io.protostuff.ByteBufferInput;
import io.protostuff.LinkedBuffer;
import io.protostuff.ProtostuffIOUtil;
import io.protostuff.Schema;
import io.protostuff.runtime.RuntimeSchema;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * @author Wang Hao
 * @date 2021/5/3 11:14
 */
public class SyncingMessageSerializer extends AbstractDistributableSerializer {

    private final Schema<SyncingMessage> schema = RuntimeSchema.getSchema(SyncingMessage.class);

    public SyncingMessageSerializer(Pool<LinkedBuffer> pool) {
        super(pool);
    }

    @Override
    public byte[] serialize(Distributable distributable, LinkedBuffer buffer) {
        return ProtostuffIOUtil.toByteArray((SyncingMessage) distributable, schema, buffer);
    }

    @Override
    public int getFactoryType() {
        return DistributableType.SYNCING;
    }

    @Override
    public Distributable create(ByteBuffer byteBuffer) {
        SyncingMessage message = new SyncingMessage();
        try {
            schema.mergeFrom(new ByteBufferInput(byteBuffer, true), message);
        } catch (IOException e) {
            throw new MessageParseException(ErrorMessage.MESSAGE_PARSE_ERROR);
        }
        return message;
    }

    @Override
    public Distributable deserialize(byte[] content, int length) {
        SyncingMessage message = new SyncingMessage();
        ProtostuffIOUtil.mergeFrom(content, 0, length, message, schema);
        return message;
    }

    @Override
    public void serialize(Distributable distributable, LinkedBuffer buffer, OutputStream outputStream)
        throws IOException {
        ProtostuffIOUtil.writeTo(outputStream, (SyncingMessage) distributable, schema, buffer);
    }
}
