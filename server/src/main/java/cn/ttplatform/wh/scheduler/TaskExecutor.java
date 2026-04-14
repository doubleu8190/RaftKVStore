package cn.ttplatform.wh.scheduler;

import java.util.concurrent.ScheduledFuture;

/**
 * @author Wang Hao
 * @date 2020/6/30 下午9:25
 */
public interface TaskExecutor {

    /**
     * Regularly perform election timeout tasks
     *
     * @param task election timeout tasks
     * @return ScheduledFuture
     */
    ScheduledFuture<?> scheduleElectionTimeoutTask(Runnable task);

    /**
     * Regularly perform log replication tasks
     *
     * @param task log replication tasks
     * @return ScheduledFuture
     */
    ScheduledFuture<?> scheduleLogReplicationTask(Runnable task);

    void execute(Runnable task);

    void executeSubTask(Runnable task);

    /**
     * Close the thread pool immediately
     */
    void close();

}
