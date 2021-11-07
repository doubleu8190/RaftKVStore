package cn.ttplatform.wh.factory;

import cn.ttplatform.wh.message.serializer.RequestVoteMessageSerializer;
import cn.ttplatform.wh.constant.DistributableType;
import cn.ttplatform.wh.message.RequestVoteMessage;
import cn.ttplatform.wh.support.FixedSizeLinkedBufferPool;
import cn.ttplatform.wh.support.Pool;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.protostuff.LinkedBuffer;
import java.nio.ByteBuffer;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Wang Hao
 * @date 2021/5/11 23:01
 */
@Slf4j
public class RequestVoteMessageFactoryTest {

    RequestVoteMessageSerializer factory;

    @Before
    public void setUp() throws Exception {
        Pool<LinkedBuffer> pool = new FixedSizeLinkedBufferPool(10);
        factory = new RequestVoteMessageSerializer(pool);
    }

    @Test
    public void getFactoryType() {
        Assert.assertEquals(DistributableType.REQUEST_VOTE, factory.getFactoryType());
    }

    @Test
    public void create() {
        RequestVoteMessage message = RequestVoteMessage.builder()
            .term(0).sourceId("A").lastLogTerm(0).lastLogIndex(0).sourceId("A").build();
        byte[] bytes = factory.serialize(message);
        long begin = System.nanoTime();
        IntStream.range(0, 10000).forEach(index -> factory.deserialize(bytes, bytes.length));
        log.info("deserialize 10000 times cost {} ns.", System.nanoTime() - begin);
    }

    @Test
    public void testCreate() {
        RequestVoteMessage message = RequestVoteMessage.builder()
            .term(0).sourceId("A").lastLogTerm(0).lastLogIndex(0).sourceId("A").build();
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
        RequestVoteMessage message = RequestVoteMessage.builder()
            .term(0).sourceId("A").lastLogTerm(0).lastLogIndex(0).sourceId("A").build();
        long begin = System.nanoTime();
        IntStream.range(0, 10000).forEach(index -> factory.serialize(message));
        log.info("serialize 10000 times cost {} ns.", System.nanoTime() - begin);
    }

    @Test
    public void testGetBytes() {
        RequestVoteMessage message = RequestVoteMessage.builder()
            .term(0).sourceId("A").lastLogTerm(0).lastLogIndex(0).sourceId("A").build();
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