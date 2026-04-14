package cn.ttplatform.wh.scheduler;

import cn.ttplatform.wh.config.ServerProperties;
import cn.ttplatform.wh.support.NamedThreadFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author Wang Hao
 * @date 2020/6/30 下午9:28
 */
@Slf4j
public class DefaultTaskExecutor implements TaskExecutor {

    private final ServerProperties properties;
    private final ScheduledExecutorService scheduler;
    private final ThreadPoolExecutor mainExecutor;
    private final ThreadPoolExecutor childExecutor;

    public DefaultTaskExecutor(ServerProperties properties) {
        this.mainExecutor = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(properties.getTaskExecutorQueueCapacity()),
                new NamedThreadFactory("core-"),
                (r, e) -> log.error("task Queue is full, reject this operation."));
        this.properties = properties;
        this.scheduler = new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("scheduler-"));
        this.childExecutor = new ThreadPoolExecutor(
                0,
                1,
                30L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(2),
                new NamedThreadFactory("subTask-"),
                (r, e) -> log.error("There is currently an executing task, reject this operation."));
    }

    @Override
    public ScheduledFuture<?> scheduleElectionTimeoutTask(Runnable task) {
        int maxElectionTimeout = properties.getMaxElectionTimeout();
        int minElectionTimeout = properties.getMinElectionTimeout();
        int timeout = ThreadLocalRandom.current().nextInt(maxElectionTimeout - minElectionTimeout) + minElectionTimeout;
        return scheduler.schedule(() -> mainExecutor.execute(task), timeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public ScheduledFuture<?> scheduleLogReplicationTask(Runnable task) {
        long delay = properties.getLogReplicationDelay();
        long interval = properties.getLogReplicationInterval();
        return scheduler.scheduleWithFixedDelay(() -> mainExecutor.execute(task), delay, interval, TimeUnit.MILLISECONDS);
    }

    @Override
    public void execute(Runnable task) {
        mainExecutor.execute(task);
    }

    @Override
    public void executeSubTask(Runnable task) {
        childExecutor.execute(task);
    }

    @Override
    public void close() {
        mainExecutor.shutdownNow();
        childExecutor.shutdownNow();
        scheduler.shutdownNow();
    }

}
