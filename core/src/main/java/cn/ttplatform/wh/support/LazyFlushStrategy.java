package cn.ttplatform.wh.support;

import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import io.netty.util.concurrent.ScheduledFuture;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

/**
 * @author Wang Hao
 * @date 2021/7/12 18:14
 */
@Slf4j
public class LazyFlushStrategy {
    private long lastFlushTime;
    private final long flushInterval;
    private final double threshold;
    private final Channel channel;

    LazyFlushStrategy(Channel channel, long flushInterval, double threshold) {
        this.channel = channel;
        this.flushInterval = flushInterval;
        this.threshold = threshold;
        beginScheduleTask();
    }

    public boolean flush() {
        long writableBytes = channel.bytesBeforeUnwritable();
        long now = System.currentTimeMillis();
        long sendBufferSize = channel.config().getOption(ChannelOption.SO_SNDBUF);
        long usedBytes = sendBufferSize - writableBytes;
        boolean timeoutCondition = now - lastFlushTime >= flushInterval && usedBytes > 0;
        boolean thresholdCondition = (double) usedBytes / sendBufferSize >= threshold;
        if (timeoutCondition || thresholdCondition) {
            lastFlushTime = now;
            return true;
        }
        return false;
    }

    private void beginScheduleTask() {
        ScheduledFuture<?> scheduledFuture = channel.eventLoop().scheduleAtFixedRate(() -> {
            if (flush()) {
                channel.flush();
            }
        }, 0, flushInterval, TimeUnit.MILLISECONDS);
        scheduledFuture.addListener((future) -> {
            if (future.isCancelled()) {
                log.debug("lazy flush strategy is cancelled.");
            }else if (future.isSuccess()) {
                log.debug("lazy flush success.");
            } else {
                log.warn("lazy flush failed. cause: ", future.cause());
            }
        });
    }
}
