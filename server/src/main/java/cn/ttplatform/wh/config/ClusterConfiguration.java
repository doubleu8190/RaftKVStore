package cn.ttplatform.wh.config;

import java.util.Properties;
import java.util.UUID;

/**
 * @author Wang Hao
 * @date 2021/11/7 22:52
 */
public class ClusterConfiguration {

    private static final String ENV_NODE_ID = "ENV_NODE_ID";
    private static final String ENV_MODE = "ENV_MODE";
    private static final String ENV_MIN_ELECTION_TIMEOUT = "ENV_MIN_ELECTION_TIMEOUT";
    private static final String ENV_MAX_ELECTION_TIMEOUT = "ENV_MAX_ELECTION_TIMEOUT";

    public void configure(Properties properties) {
        properties.putIfAbsent("nodeId", System.getProperty(ENV_NODE_ID, UUID.randomUUID().toString()));
        properties.putIfAbsent("mode", System.getProperty(ENV_MODE, RunMode.SINGLETON.toString()));
        properties.putIfAbsent("minElectionTimeout", System.getProperty(ENV_MIN_ELECTION_TIMEOUT, "3000"));
        properties.putIfAbsent("maxElectionTimeout", System.getProperty(ENV_MAX_ELECTION_TIMEOUT, "4000"));
    }
}
