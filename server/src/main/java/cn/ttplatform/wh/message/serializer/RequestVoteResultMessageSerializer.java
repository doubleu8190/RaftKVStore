package cn.ttplatform.wh.message.serializer;

import cn.ttplatform.wh.message.RequestVoteResultMessage;
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
 * @date 2021/2/18 16:21
 */
public class RequestVoteResultMessageSerializer extends AbstractDistributableSerializer {

    private final Schema<RequestVoteResultMessage> schema = RuntimeSchema.getSchema(RequestVoteResultMessage.class);

    public RequestVoteResultMessageSerializer(Pool<LinkedBuffer> pool) {
        super(pool);
    }

    @Override
    public int getFactoryType() {
        return DistributableType.REQUEST_VOTE_RESULT;
    }

    @Override
    public Distributable deserialize(byte[] content, int length) {
        RequestVoteResultMessage message = new RequestVoteResultMessage();
        ProtostuffIOUtil.mergeFrom(content, 0, length, message, schema);
        return message;
    }

    @Override
    public Distributable create(ByteBuffer byteBuffer) {
        RequestVoteResultMessage message = new RequestVoteResultMessage();
        try {
            schema.mergeFrom(new ByteBufferInput(byteBuffer, true), message);
        } catch (IOException e) {
            throw new MessageParseException(ErrorMessage.MESSAGE_PARSE_ERROR);
        }
        return message;
    }

    @Override
    public byte[] serialize(Distributable distributable, LinkedBuffer buffer) {
        return ProtostuffIOUtil.toByteArray((RequestVoteResultMessage) distributable, schema, buffer);
    }

    @Override
    public void serialize(Distributable distributable, LinkedBuffer buffer, OutputStream outputStream)
        throws IOException {
        ProtostuffIOUtil.writeTo(outputStream, (RequestVoteResultMessage) distributable, schema, buffer);
    }
}
