package cn.ttplatform.wh.config;

import java.util.Properties;
import java.util.UUID;

/**
 * @author Wang Hao
 * @date 2021/11/7 22:52
 */
public class ClusterConfiguration {

    public void configure(Properties properties) {
        properties.putIfAbsent("nodeId", UUID.randomUUID().toString());
        properties.putIfAbsent("mode", RunMode.SINGLETON.toString());
        properties.putIfAbsent("minElectionTimeout", "3000");
        properties.putIfAbsent("maxElectionTimeout", "4000");
    }
}
