package cn.ttplatform.wh.scheduler;

import cn.ttplatform.wh.config.ServerProperties;
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
public class SingleThreadScheduler implements Scheduler {

    private final ServerProperties properties;
    private final ScheduledExecutorService scheduler;
    private final ThreadPoolExecutor executor;

    public SingleThreadScheduler(ServerProperties properties, ThreadPoolExecutor executor) {
        this.properties = properties;
        this.scheduler = new ScheduledThreadPoolExecutor(1, r -> new Thread(r, "scheduler"));
        this.executor = executor;
    }

    @Override
    public ScheduledFuture<?> scheduleElectionTimeoutTask(Runnable task) {
        int maxElectionTimeout = properties.getMaxElectionTimeout();
        int minElectionTimeout = properties.getMinElectionTimeout();
        int timeout = ThreadLocalRandom.current().nextInt(maxElectionTimeout - minElectionTimeout) + minElectionTimeout;
        return scheduler.schedule(createTask(task), timeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public ScheduledFuture<?> scheduleLogReplicationTask(Runnable task) {
        long delay = properties.getLogReplicationDelay();
        long interval = properties.getLogReplicationInterval();
        return scheduler.scheduleWithFixedDelay(createTask(task), delay, interval, TimeUnit.MILLISECONDS);
    }

    public Runnable createTask(Runnable task) {
        return () -> executor.execute(task);
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
    }

}
