package cn.ttplatform.wh.cmd.factory;

import cn.ttplatform.wh.cmd.SetCommand;
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
 * @date 2021/3/15 15:56
 */
public class SetCommandSerializer extends AbstractDistributableSerializer {

    private final Schema<SetCommand> schema = RuntimeSchema.getSchema(SetCommand.class);

    public SetCommandSerializer(Pool<LinkedBuffer> pool) {
        super(pool);
    }

    @Override
    public int getFactoryType() {
        return DistributableType.SET_COMMAND;
    }

    @Override
    public Distributable deserialize(byte[] content, int length) {
        SetCommand command = new SetCommand();
        ProtostuffIOUtil.mergeFrom(content, 0, length, command, schema);
        return command;
    }

    @Override
    public Distributable create(ByteBuffer byteBuffer) {
        SetCommand cmd = new SetCommand();
        try {
            schema.mergeFrom(new ByteBufferInput(byteBuffer, true), cmd);
        } catch (IOException e) {
            throw new MessageParseException(ErrorMessage.MESSAGE_PARSE_ERROR);
        }
        return cmd;
    }

    @Override
    public byte[] serialize(Distributable message, LinkedBuffer buffer) {
        return ProtostuffIOUtil.toByteArray((SetCommand) message, schema, buffer);
    }

    @Override
    public void serialize(Distributable distributable, LinkedBuffer buffer, OutputStream outputStream)
        throws IOException {
        ProtostuffIOUtil.writeTo(outputStream, (SetCommand) distributable, schema, buffer);
    }
}
