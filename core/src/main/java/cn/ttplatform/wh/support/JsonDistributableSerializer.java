package cn.ttplatform.wh.support;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * A JSON-based serializer for Distributable objects.
 * This serializer uses Gson library for JSON serialization/deserialization.
 *
 * @author wanghao
 * @date 2026-04-14
 */
@Slf4j
public class JsonDistributableSerializer implements DistributableSerializer {

    private final Gson gson;
    private final Class<? extends Distributable> targetClass;
    private final byte factoryType;

    /**
     * Creates a JSON serializer for a specific Distributable type.
     *
     * @param targetClass the concrete Distributable class this serializer handles
     * @param factoryType the type byte that identifies this Distributable type
     */
    public JsonDistributableSerializer(
            Class<? extends Distributable> targetClass,
            byte factoryType) {
        this.targetClass = targetClass;
        this.factoryType = factoryType;
        this.gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .create();
    }

    @Override
    public byte getFactoryType() {
        return factoryType;
    }

    @Override
    public Distributable deserialize(byte[] content, int contentLength) throws JsonSyntaxException {
        String json = new String(content, 0, contentLength, StandardCharsets.UTF_8);
        return gson.fromJson(json, targetClass);
    }

    @Override
    public Distributable deserialize(ByteBuffer byteBuffer, int contentLength) throws JsonSyntaxException {
        ByteBuffer slice = byteBuffer.slice();
        slice.limit(contentLength);
        byte[] bytes = new byte[contentLength];
        slice.get(bytes);
        String json = new String(bytes, StandardCharsets.UTF_8);
        return gson.fromJson(json, targetClass);
    }

    @Override
    public void serialize(Distributable obj, ByteBuf byteBuffer) {
        // Write the factory type first
        byteBuffer.writeByte(getFactoryType());

        // Save position for content length placeholder
        int lengthIndex = byteBuffer.writerIndex();
        byteBuffer.writeInt(0); // placeholder for content length

        byteBuffer.writeBytes(serialize(obj));

        // Update content length
        int contentLength = byteBuffer.writerIndex() - lengthIndex - 4;
        byteBuffer.setInt(lengthIndex, contentLength);
    }

    @Override
    public byte[] serialize(Distributable obj) {
        return gson.toJson(obj).getBytes(StandardCharsets.UTF_8);
    }

}