package cn.ttplatform.wh.cmd.factory;

import cn.ttplatform.wh.cmd.RequestFailedCommand;
import cn.ttplatform.wh.constant.DistributableType;
import cn.ttplatform.wh.support.FixedSizeLinkedBufferPool;
import cn.ttplatform.wh.support.Pool;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.protostuff.LinkedBuffer;
import java.nio.ByteBuffer;
import java.util.UUID;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Wang Hao
 * @date 2021/5/11 23:22
 */
@Slf4j
public class RequestFailedCommandSerializerTest {

    RequestFailedCommandSerializer factory;

    @Before
    public void setUp() throws Exception {
        Pool<LinkedBuffer> pool = new FixedSizeLinkedBufferPool(10);
        factory = new RequestFailedCommandSerializer(pool);
    }

    @Test
    public void getFactoryType() {
        Assert.assertEquals(DistributableType.REQUEST_FAILED_COMMAND, factory.getFactoryType());
    }

    @Test
    public void create() {
        RequestFailedCommand message = RequestFailedCommand.builder().id(UUID.randomUUID().toString()).failedMessage("").build();
        byte[] bytes = factory.serialize(message);
        long begin = System.nanoTime();
        IntStream.range(0, 10000).forEach(index -> factory.deserialize(bytes, bytes.length));
        log.info("deserialize 10000 times cost {} ns.", System.nanoTime() - begin);
    }

    @Test
    public void testCreate() {
        RequestFailedCommand message = RequestFailedCommand.builder().id(UUID.randomUUID().toString()).failedMessage("").build();
        byte[] bytes = factory.serialize(message);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bytes.length);
        byteBuffer.put(bytes);
        byteBuffer.flip();
        long begin = System.nanoTime();
        IntStream.range(0, 10000).forEach(index -> {
            factory.deserialize(byteBuffer, bytes.length);
            byteBuffer.position(0);
        });
        log.info("deserialize 10000 times cost {} ns.", System.nanoTime() - begin);
    }

    @Test
    public void getBytes() {
        RequestFailedCommand message = RequestFailedCommand.builder().id(UUID.randomUUID().toString()).failedMessage("").build();
        long begin = System.nanoTime();
        IntStream.range(0, 10000).forEach(index -> factory.serialize(message));
        log.info("serialize 10000 times cost {} ns.", System.nanoTime() - begin);
    }

    @Test
    public void testGetBytes() {
        RequestFailedCommand message = RequestFailedCommand.builder().id(UUID.randomUUID().toString()).failedMessage("").build();
        UnpooledByteBufAllocator allocator = new UnpooledByteBufAllocator(true);
        ByteBuf byteBuf = allocator.directBuffer();
        long begin = System.nanoTime();
        IntStream.range(0, 10000).forEach(index -> {
            factory.serialize(message, byteBuf);
            byteBuf.clear();
        });
        log.info("serialize 10000 times cost {} ns.", System.nanoTime() - begin);
    }
}