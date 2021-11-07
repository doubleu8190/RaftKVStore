package cn.ttplatform.wh.cmd.factory;

import cn.ttplatform.wh.cmd.ClusterChangeResultCommand;
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
 * @date 2021/4/24 11:30
 */
public class ClusterChangeResultCommandSerializer extends AbstractDistributableSerializer {

    private final Schema<ClusterChangeResultCommand> schema = RuntimeSchema.getSchema(ClusterChangeResultCommand.class);

    public ClusterChangeResultCommandSerializer(Pool<LinkedBuffer> pool) {
        super(pool);
    }

    @Override
    public int getFactoryType() {
        return DistributableType.CLUSTER_CHANGE_RESULT_COMMAND;
    }

    @Override
    public Distributable deserialize(byte[] content, int length) {
        ClusterChangeResultCommand command = new ClusterChangeResultCommand();
        ProtostuffIOUtil.mergeFrom(content, 0, length, command, schema);
        return command;
    }

    @Override
    public Distributable create(ByteBuffer byteBuffer) {
        ClusterChangeResultCommand cmd = new ClusterChangeResultCommand();
        try {
            schema.mergeFrom(new ByteBufferInput(byteBuffer, true), cmd);
        } catch (IOException e) {
            throw new MessageParseException(ErrorMessage.MESSAGE_PARSE_ERROR);
        }
        return cmd;
    }

    @Override
    public byte[] serialize(Distributable distributable, LinkedBuffer buffer) {
        return ProtostuffIOUtil.toByteArray((ClusterChangeResultCommand) distributable, schema, buffer);
    }

    @Override
    public void serialize(Distributable distributable, LinkedBuffer buffer, OutputStream outputStream)
        throws IOException {
        ProtostuffIOUtil.writeTo(outputStream, (ClusterChangeResultCommand) distributable, schema, buffer);
    }
}
