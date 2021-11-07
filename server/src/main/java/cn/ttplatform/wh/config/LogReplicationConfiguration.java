package cn.ttplatform.wh.config;

import java.util.Properties;

/**
 * @author Wang Hao
 * @date 2021/11/7 22:45
 */
public class LogReplicationConfiguration {

    public void configure(Properties properties) {
        properties.putIfAbsent("logReplicationDelay", "1000");
        properties.putIfAbsent("logReplicationInterval", "1000");
        properties.putIfAbsent("retryTimeout", "900");
        properties.putIfAbsent("maxTransferLogs", "10000");
        properties.putIfAbsent("maxTransferSize", "10240");
    }
}
