package cn.ttplatform.wh.config;

import java.util.Properties;

/**
 * @author Wang Hao
 * @date 2021/11/7 22:33
 */
public class LogStorageConfiguration {

    private static final String ENV_SNAPSHOT_GENERATE_THRESHOLD = "ENV_SNAPSHOT_GENERATE_THRESHOLD";
    private static final String ENV_USE_DIRECT_BYTEBUFFER = "ENV_USE_DIRECT_BYTEBUFFER";
    private static final String ENV_LINKED_BUFF_POOL_SIZE = "ENV_LINKED_BUFF_POOL_SIZE";
    private static final String ENV_BYTEBUFFER_POOL_SIZE = "ENV_BYTEBUFFER_POOL_SIZE";
    private static final String ENV_BYTEBUFFER_SIZE_LIMIT = "ENV_BYTEBUFFER_SIZE_LIMIT";
    private static final String ENV_SYN_LOG_FLUSH = "ENV_SYN_LOG_FLUSH";
    private static final String ENV_BLOCK_SIZE = "ENV_BLOCK_SIZE";
    private static final String ENV_BLOCK_FLUSH_INTERVAL = "ENV_BLOCK_FLUSH_INTERVAL";
    private static final String ENV_BLOCK_CACHE_SIZE = "ENV_BLOCK_CACHE_SIZE";
    private static final String ENV_LOG_INDEX_CACHE_SIZE = "ENV_LOG_INDEX_CACHE_SIZE";
    private static final String ENV_BASE_PATH = "ENV_BASE_PATH";

    public void configure(Properties properties) {
        properties.putIfAbsent("snapshotGenerateThreshold", System.getProperty(ENV_SNAPSHOT_GENERATE_THRESHOLD, String.valueOf(1024 * 1024 * 10)));
        properties.putIfAbsent("useDirectByteBuffer", System.getProperty(ENV_USE_DIRECT_BYTEBUFFER, "true"));
        properties.putIfAbsent("linkedBuffPoolSize", System.getProperty(ENV_LINKED_BUFF_POOL_SIZE, "16"));
        properties.putIfAbsent("byteBufferPoolSize", System.getProperty(ENV_BYTEBUFFER_POOL_SIZE, "10"));
        properties.putIfAbsent("byteBufferSizeLimit", System.getProperty(ENV_BYTEBUFFER_SIZE_LIMIT, String.valueOf(1024 * 1024 * 16)));
        properties.putIfAbsent("synLogFlush", System.getProperty(ENV_SYN_LOG_FLUSH, "false"));
        properties.putIfAbsent("blockSize", System.getProperty(ENV_BLOCK_SIZE, String.valueOf(1024 * 1024 * 4)));
        properties.putIfAbsent("blockFlushInterval", System.getProperty(ENV_BLOCK_FLUSH_INTERVAL, "1000"));
        properties.putIfAbsent("blockCacheSize", System.getProperty(ENV_BLOCK_CACHE_SIZE, "50"));
        properties.putIfAbsent("logIndexCacheSize", System.getProperty(ENV_LOG_INDEX_CACHE_SIZE, "100"));
        properties.putIfAbsent("basePath", System.getProperty(ENV_BASE_PATH, System.getProperty("user.home")));
    }
}
