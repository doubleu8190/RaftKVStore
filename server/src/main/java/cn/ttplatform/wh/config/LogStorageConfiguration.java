package cn.ttplatform.wh.config;

import java.util.Properties;

/**
 * @author Wang Hao
 * @date 2021/11/7 22:33
 */
public class LogStorageConfiguration {

    public void configure(Properties properties) {
        properties.putIfAbsent("snapshotGenerateThreshold", String.valueOf(1024 * 1024 * 10));
        properties.putIfAbsent("useDirectByteBuffer", "true");
        properties.putIfAbsent("linkedBuffPollSize", "16");
        properties.putIfAbsent("byteBufferPoolSize", "10");
        properties.putIfAbsent("byteBufferSizeLimit", String.valueOf(1024 * 1024 * 10));
        properties.putIfAbsent("synLogFlush", "false");
        properties.putIfAbsent("blockSize", String.valueOf(1024 * 1024 * 4));
        properties.putIfAbsent("blockFlushInterval", "1000");
        properties.putIfAbsent("blockCacheSize", "50");
        properties.putIfAbsent("logIndexCacheSize", "100");
        properties.putIfAbsent("basePath", System.getProperty("user.home"));
    }
}
