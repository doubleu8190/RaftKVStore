package cn.ttplatform.wh.config;

import java.util.Properties;

/**
 * @author Wang Hao
 * @date 2021/11/7 22:22
 */
public class NettyConfiguration {

    private static final String DEFAULT_HOST = "127.0.0.1";
    private static final String DEFAULT_CMD_PORT = "8190";
    private static final String DEFAULT_CONNECTOR_PORT = "1013";

    public void configure(Properties properties){
        properties.putIfAbsent("host",DEFAULT_HOST);
        properties.putIfAbsent("port",DEFAULT_CMD_PORT);
        properties.putIfAbsent("connectorHost",DEFAULT_HOST);
        properties.putIfAbsent("connectorPort",DEFAULT_CONNECTOR_PORT);
        properties.putIfAbsent("readIdleTimeout","10");
        properties.putIfAbsent("writeIdleTimeout","10");
        properties.putIfAbsent("allIdleTimeout","10");
        properties.putIfAbsent("lazyFlushInterval","100");
        properties.putIfAbsent("lazyFlushThreshold",String.valueOf((double) 1 / 1000));
    }

}
