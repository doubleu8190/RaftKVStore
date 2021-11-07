package cn.ttplatform.wh.factory;

import cn.ttplatform.wh.message.serializer.InstallSnapshotMessageSerializer;
import cn.ttplatform.wh.constant.DistributableType;
import cn.ttplatform.wh.message.InstallSnapshotMessage;
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
 * @date 2021/5/11 22:50
 */
@Slf4j
public class InstallSnapshotMessageSerializerTest {

    InstallSnapshotMessageSerializer factory;

    @Before
    public void setUp() throws Exception {
        Pool<LinkedBuffer> pool = new FixedSizeLinkedBufferPool(10);
        factory = new InstallSnapshotMessageSerializer(pool);
    }

    @Test
    public void getFactoryType() {
        Assert.assertEquals(DistributableType.INSTALL_SNAPSHOT, factory.getFactoryType());
    }

    @Test
    public void create() {
        InstallSnapshotMessage message = InstallSnapshotMessage.builder()
            .term(0).lastIncludeTerm(0).lastIncludeIndex(0).chunk(new byte[0]).offset(0).sourceId("A").build();
        byte[] bytes = factory.serialize(message);
        long begin = System.nanoTime();
        IntStream.range(0, 10000).forEach(index -> factory.deserialize(bytes, bytes.length));
        log.info("deserialize 10000 times cost {} ns.", System.nanoTime() - begin);
    }

    @Test
    public void testCreate() {
        InstallSnapshotMessage message = InstallSnapshotMessage.builder()
            .term(0).lastIncludeTerm(0).lastIncludeIndex(0).chunk(new byte[0]).offset(0).sourceId("A").build();
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
        InstallSnapshotMessage message = InstallSnapshotMessage.builder()
            .term(0).lastIncludeTerm(0).lastIncludeIndex(0).chunk(new byte[0]).offset(0).sourceId("A").build();
        long begin = System.nanoTime();
        IntStream.range(0, 10000).forEach(index -> factory.serialize(message));
        log.info("serialize 10000 times cost {} ns.", System.nanoTime() - begin);
    }

    @Test
    public void testGetBytes() {
        InstallSnapshotMessage message = InstallSnapshotMessage.builder()
            .term(0).lastIncludeTerm(0).lastIncludeIndex(0).chunk(new byte[0]).offset(0).sourceId("A").build();
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