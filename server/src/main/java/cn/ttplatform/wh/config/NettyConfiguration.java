package cn.ttplatform.wh.config;

import java.util.Properties;

/**
 * @author Wang Hao
 * @date 2021/11/7 22:22
 */
public class NettyConfiguration {

    private static final String ENV_HOST = "ENV_HOST";
    private static final String ENV_CMD_PORT = "ENV_CMD_PORT";
    private static final String ENV_CONNECTOR_PORT = "ENV_CONNECTOR_PORT";
    private static final String ENV_READ_IDLE_TIMEOUT = "ENV_READ_IDLE_TIMEOUT";
    private static final String ENV_WRITE_IDLE_TIMEOUT = "ENV_WRITE_IDLE_TIMEOUT";
    private static final String ENV_ALL_IDLE_TIMEOUT = "ENV_ALL_IDLE_TIMEOUT";
    private static final String ENV_BACKLOG = "ENV_BACKLOG";
    private static final String ENV_TCP_NO_DELAY = "ENV_TCP_NO_DELAY";
    private static final String ENV_LAZY_FLUSH_INTERVAL = "ENV_LAZY_FLUSH_INTERVAL";
    private static final String ENV_LAZY_FLUSH_THRESHOLD = "ENV_LAZY_FLUSH_THRESHOLD";


    public void configure(Properties properties) {
        properties.putIfAbsent("host", System.getProperty(ENV_HOST, "127.0.0.1"));
        properties.putIfAbsent("port", System.getProperty(ENV_CMD_PORT, "8190"));
        properties.putIfAbsent("connectorHost", System.getProperty(ENV_HOST, "127.0.0.1"));
        properties.putIfAbsent("connectorPort", System.getProperty(ENV_CONNECTOR_PORT, "1013"));
        properties.putIfAbsent("readIdleTimeout", System.getProperty(ENV_READ_IDLE_TIMEOUT, "0"));
        properties.putIfAbsent("writeIdleTimeout", System.getProperty(ENV_WRITE_IDLE_TIMEOUT, "0"));
        properties.putIfAbsent("allIdleTimeout", System.getProperty(ENV_ALL_IDLE_TIMEOUT, "30"));
        properties.putIfAbsent("backlog", System.getProperty(ENV_BACKLOG, "1024"));
        properties.putIfAbsent("tcpNoDelay", System.getProperty(ENV_TCP_NO_DELAY, "false"));
        properties.putIfAbsent("lazyFlushInterval", System.getProperty(ENV_LAZY_FLUSH_INTERVAL, "100"));
        properties.putIfAbsent("lazyFlushThreshold", System.getProperty(ENV_LAZY_FLUSH_THRESHOLD, String.valueOf((double) 1 / 1000)));
    }

}
