package cn.ttplatform.wh.cmd.factory;

import cn.ttplatform.wh.cmd.RedirectCommand;
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
 * @date 2021/5/11 23:20
 */
@Slf4j
public class RedirectCommandFactoryTest {

    RedirectCommandSerializer factory;

    @Before
    public void setUp() throws Exception {
        Pool<LinkedBuffer> pool = new FixedSizeLinkedBufferPool(10);
        factory = new RedirectCommandSerializer(pool);
    }

    @Test
    public void getFactoryType() {
        Assert.assertEquals(DistributableType.REDIRECT_COMMAND, factory.getFactoryType());
    }

    @Test
    public void create() {
        RedirectCommand message = RedirectCommand.builder().id(UUID.randomUUID().toString()).leader("A").endpointMetaData("")
            .build();
        byte[] bytes = factory.serialize(message);
        long begin = System.nanoTime();
        IntStream.range(0, 10000).forEach(index -> factory.deserialize(bytes, bytes.length));
        log.info("deserialize 10000 times cost {} ns.", System.nanoTime() - begin);
    }

    @Test
    public void testCreate() {
        RedirectCommand message = RedirectCommand.builder().id(UUID.randomUUID().toString()).leader("A").endpointMetaData("")
            .build();
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
        RedirectCommand message = RedirectCommand.builder().id(UUID.randomUUID().toString()).leader("A").endpointMetaData("")
            .build();
        long begin = System.nanoTime();
        IntStream.range(0, 10000).forEach(index -> factory.serialize(message));
        log.info("serialize 10000 times cost {} ns.", System.nanoTime() - begin);
    }

    @Test
    public void testGetBytes() {
        RedirectCommand message = RedirectCommand.builder().id(UUID.randomUUID().toString()).leader("A").endpointMetaData("")
            .build();
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