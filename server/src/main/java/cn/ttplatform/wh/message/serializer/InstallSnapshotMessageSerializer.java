package cn.ttplatform.wh.message.serializer;

import cn.ttplatform.wh.message.InstallSnapshotMessage;
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
 * @date 2021/2/18 16:11
 */
public class InstallSnapshotMessageSerializer extends AbstractDistributableSerializer {

    private final Schema<InstallSnapshotMessage> schema = RuntimeSchema.getSchema(InstallSnapshotMessage.class);

    public InstallSnapshotMessageSerializer(Pool<LinkedBuffer> pool) {
        super(pool);
    }

    @Override
    public int getFactoryType() {
        return DistributableType.INSTALL_SNAPSHOT;
    }

    @Override
    public Distributable deserialize(byte[] content, int length) {
        InstallSnapshotMessage message = new InstallSnapshotMessage();
        ProtostuffIOUtil.mergeFrom(content, 0, length, message, schema);
        return message;
    }

    @Override
    public Distributable create(ByteBuffer byteBuffer) {
        InstallSnapshotMessage message = new InstallSnapshotMessage();
        try {
            schema.mergeFrom(new ByteBufferInput(byteBuffer, true), message);
        } catch (IOException e) {
            throw new MessageParseException(ErrorMessage.MESSAGE_PARSE_ERROR);
        }
        return message;
    }

    @Override
    public byte[] serialize(Distributable distributable, LinkedBuffer buffer) {
        return ProtostuffIOUtil.toByteArray((InstallSnapshotMessage) distributable, schema, buffer);
    }

    @Override
    public void serialize(Distributable distributable, LinkedBuffer buffer, OutputStream outputStream)
        throws IOException {
        ProtostuffIOUtil.writeTo(outputStream, (InstallSnapshotMessage) distributable, schema, buffer);
    }
}
