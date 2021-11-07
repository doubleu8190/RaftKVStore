package cn.ttplatform.wh.factory;

import cn.ttplatform.wh.message.serializer.AppendLogEntriesMessageSerializer;
import cn.ttplatform.wh.constant.DistributableType;
import cn.ttplatform.wh.message.AppendLogEntriesMessage;
import cn.ttplatform.wh.support.FixedSizeLinkedBufferPool;
import cn.ttplatform.wh.support.Pool;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.protostuff.LinkedBuffer;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.stream.IntStream;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Wang Hao
 * @date 2021/5/11 10:59
 */
@Slf4j
public class AppendLogEntriesMessageSerializerTest {

    AppendLogEntriesMessageSerializer factory;

    @Before
    public void setUp() throws Exception {
        Pool<LinkedBuffer> pool = new FixedSizeLinkedBufferPool(10);
        factory = new AppendLogEntriesMessageSerializer(pool);
    }

    @Test
    public void getFactoryType() {
        Assert.assertEquals(DistributableType.APPEND_LOG_ENTRIES, factory.getFactoryType());
    }

    @Test
    public void create() {
        AppendLogEntriesMessage appendLogEntriesMessage = AppendLogEntriesMessage.builder().matchComplete(true).preLogTerm(1)
            .preLogIndex(1)
            .leaderCommitIndex(1).sourceId("A")
            .sourceId("A").term(1).logs(Collections.emptyList()).build();
        byte[] bytes = factory.serialize(appendLogEntriesMessage);
        long begin = System.nanoTime();
        IntStream.range(0, 10000).forEach(index -> factory.deserialize(bytes, bytes.length));
        log.info("deserialize 10000 times cost {} ns.", System.nanoTime() - begin);
    }

    @Test
    public void testCreate() {
        AppendLogEntriesMessage appendLogEntriesMessage = AppendLogEntriesMessage.builder().matchComplete(true).preLogTerm(1)
            .preLogIndex(1)
            .leaderCommitIndex(1).sourceId("A")
            .sourceId("A").term(1).logs(Collections.emptyList()).build();
        byte[] bytes = factory.serialize(appendLogEntriesMessage);
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
        AppendLogEntriesMessage appendLogEntriesMessage = AppendLogEntriesMessage.builder().matchComplete(true).preLogTerm(1)
            .preLogIndex(1)
            .leaderCommitIndex(1).sourceId("A")
            .sourceId("A").term(1).logs(Collections.emptyList()).build();
        long begin = System.nanoTime();
        IntStream.range(0, 10000).forEach(index -> factory.serialize(appendLogEntriesMessage));
        log.info("serialize 10000 times cost {} ns.", System.nanoTime() - begin);
    }

    @Test
    public void testGetBytes() {
        AppendLogEntriesMessage appendLogEntriesMessage = AppendLogEntriesMessage.builder().matchComplete(true).preLogTerm(1)
            .preLogIndex(1)
            .leaderCommitIndex(1).sourceId("A")
            .sourceId("A").term(1).logs(Collections.emptyList()).build();
        UnpooledByteBufAllocator allocator = new UnpooledByteBufAllocator(true);
        ByteBuf byteBuf = allocator.directBuffer();
        long begin = System.nanoTime();
        IntStream.range(0, 10000).forEach(index -> {
            factory.serialize(appendLogEntriesMessage,  byteBuf);
            byteBuf.clear();
        });
        log.info("serialize 10000 times cost {} ns.", System.nanoTime() - begin);

    }
}