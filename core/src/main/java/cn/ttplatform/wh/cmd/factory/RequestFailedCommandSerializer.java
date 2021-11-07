package cn.ttplatform.wh.cmd.factory;

import cn.ttplatform.wh.cmd.RequestFailedCommand;
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
 * @date 2021/4/24 20:37
 */
public class RequestFailedCommandSerializer extends AbstractDistributableSerializer {

    private final Schema<RequestFailedCommand> schema = RuntimeSchema.getSchema(RequestFailedCommand.class);

    public RequestFailedCommandSerializer(Pool<LinkedBuffer> pool) {
        super(pool);
    }

    @Override
    public int getFactoryType() {
        return DistributableType.REQUEST_FAILED_COMMAND;
    }

    @Override
    public Distributable deserialize(byte[] content, int length) {
        RequestFailedCommand command = new RequestFailedCommand();
        ProtostuffIOUtil.mergeFrom(content, 0, length, command, schema);
        return command;
    }

    @Override
    public Distributable create(ByteBuffer byteBuffer) {
        RequestFailedCommand cmd = new RequestFailedCommand();
        try {
            schema.mergeFrom(new ByteBufferInput(byteBuffer, true), cmd);
        } catch (IOException e) {
            throw new MessageParseException(ErrorMessage.MESSAGE_PARSE_ERROR);
        }
        return cmd;
    }

    @Override
    public byte[] serialize(Distributable distributable, LinkedBuffer buffer) {
        return ProtostuffIOUtil.toByteArray((RequestFailedCommand) distributable, schema, buffer);
    }

    @Override
    public void serialize(Distributable distributable, LinkedBuffer buffer, OutputStream outputStream)
        throws IOException {
        ProtostuffIOUtil.writeTo(outputStream, (RequestFailedCommand) distributable, schema, buffer);
    }

}
