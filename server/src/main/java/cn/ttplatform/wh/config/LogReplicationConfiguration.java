package cn.ttplatform.wh.config;

import java.util.Properties;

/**
 * @author Wang Hao
 * @date 2021/11/7 22:45
 */
public class LogReplicationConfiguration {

    private static final String ENV_LOG_REPLICATION_DELAY = "ENV_LOG_REPLICATION_DELAY";
    private static final String ENV_LOG_REPLICATION_INTERVAL = "ENV_log_Replication_Interval";
    private static final String ENV_RETRY_TIMEOUT = "ENV_RETRY_TIMEOUT";
    private static final String ENV_MAX_TRANSFER_LOGS = "ENV_MAX_TRANSFER_LOGS";
    private static final String ENV_MAX_TRANSFER_SIZE = "ENV_MAX_TRANSFER_SIZE";

    public void configure(Properties properties) {
        properties.putIfAbsent("logReplicationDelay", System.getProperty(ENV_LOG_REPLICATION_DELAY, "1000"));
        properties.putIfAbsent("logReplicationInterval", System.getProperty(ENV_LOG_REPLICATION_INTERVAL, "1000"));
        properties.putIfAbsent("retryTimeout", System.getProperty(ENV_RETRY_TIMEOUT, "900"));
        properties.putIfAbsent("maxTransferLogs", System.getProperty(ENV_MAX_TRANSFER_LOGS, "10000"));
        properties.putIfAbsent("maxTransferSize", System.getProperty(ENV_MAX_TRANSFER_SIZE, "10240"));
    }
}
