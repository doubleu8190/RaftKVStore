package cn.ttplatform.wh.config;

import cn.ttplatform.wh.exception.OperateFileException;
import io.netty.channel.EventLoopGroup;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import lombok.Data;

/**
 * @author Wang Hao
 * @date 2020/6/30 下午9:30
 */
@Data
public class ServerProperties {

    /**
     * an unique id
     */
    private String nodeId;

    private RunMode mode;

    private String clusterInfo;

    /**
     * The service will listen for connections from this host
     */
    private String host;

    /**
     * The service will listen for connections from this port
     */
    private int port;

    private String connectorHost;

    private int connectorPort;

    /**
     * the number of thread used in server side {@link EventLoopGroup}
     */
    private int bossThreads;

    /**
     * the number of thread used in server side {@link EventLoopGroup}
     */
    private int workerThreads;

    private int backlog;

    private boolean tcpNoDelay;

    /**
     * Minimum election timeout
     */
    private int minElectionTimeout;

    /**
     * Maximum election timeout
     */
    private int maxElectionTimeout;

    /**
     * Replication log task will start in {@code logReplicationDelay} milliseconds
     */
    private long logReplicationDelay;

    /**
     * The task will be executed every {@code logReplicationInterval} milliseconds6
     */
    private long logReplicationInterval;

    /**
     * replicationHeartBeat should less than {@code logReplicationInterval} milliseconds6
     */
    private long retryTimeout;

    /**
     * all the data will be stored in {@code basePath}
     */
    private File base;

    /**
     * Each {@code snapshotGenerateThreshold} logs added generates a snapshot of the logs
     */
    private int snapshotGenerateThreshold;

    /**
     * The maximum number of transmission logs
     */
    private int maxTransferLogs;

    /**
     * The maximum number of transmission byte size
     */
    private int maxTransferSize;

    /**
     * {@code linkedBuffPollSize} used for serialize/deserialize obj
     */
    private int linkedBuffPoolSize;

    private int readIdleTimeout;

    private int writeIdleTimeout;

    private int allIdleTimeout;

    private boolean useDirectByteBuffer;

    private int byteBufferPoolSize;

    private int byteBufferSizeLimit;

    private boolean synLogFlush;

    /**
     * only used when synLogFlush is false
     */
    private int blockCacheSize;
    /**
     * only used when synLogFlush is false
     */
    private int blockSize;
    /**
     * only used when synLogFlush is false
     */
    private long blockFlushInterval;

    private int logIndexCacheSize;

    private long lazyFlushInterval;

    private double lazyFlushThreshold;

    public ServerProperties() {
        Properties properties = new Properties();
        configure(properties);
        afterConfigure(properties);
    }

    public ServerProperties(String configPath) {
        Properties properties = new Properties();
        File file = new File(configPath);
        try (FileInputStream fis = new FileInputStream(file)) {
            properties.load(fis);
        } catch (IOException e) {
            throw new OperateFileException(e.getMessage());
        }
        configure(properties);
        afterConfigure(properties);
    }

    private void configure(Properties properties) {
        ClusterConfiguration clusterConfiguration = new ClusterConfiguration();
        clusterConfiguration.configure(properties);
        NettyConfiguration nettyConfiguration = new NettyConfiguration();
        nettyConfiguration.configure(properties);
        LogReplicationConfiguration logReplicationConfiguration = new LogReplicationConfiguration();
        logReplicationConfiguration.configure(properties);
        LogStorageConfiguration logStorageConfiguration = new LogStorageConfiguration();
        logStorageConfiguration.configure(properties);
        Log4jConfiguration log4jConfiguration = new Log4jConfiguration();
        log4jConfiguration.configure(properties);
    }

    private void afterConfigure(Properties properties) {
        nodeId = properties.getProperty("nodeId");
        String modeProperty = properties.getProperty("mode");
        if (RunMode.SINGLETON.toString().equals(modeProperty)) {
            mode = RunMode.SINGLETON;
        } else {
            mode = RunMode.CLUSTER;
            clusterInfo = properties.getProperty("clusterInfo");
        }
        host = properties.getProperty("host");
        port = Integer.parseInt(properties.getProperty("port"));
        connectorHost = properties.getProperty("connectorHost");
        connectorPort = Integer.parseInt(properties.getProperty("connectorPort"));
        bossThreads = Integer.parseInt(properties.getProperty("bossThreads"));
        workerThreads = Integer.parseInt(properties.getProperty("workerThreads"));
        backlog = Integer.parseInt(properties.getProperty("backlog"));
        tcpNoDelay = Boolean.parseBoolean(properties.getProperty("tcpNoDelay"));
        minElectionTimeout = Integer.parseInt(properties.getProperty("minElectionTimeout"));
        maxElectionTimeout = Integer.parseInt(properties.getProperty("maxElectionTimeout"));
        logReplicationDelay = Long.parseLong(properties.getProperty("logReplicationDelay"));
        logReplicationInterval = Long.parseLong(properties.getProperty("logReplicationInterval"));
        retryTimeout = Long.parseLong(properties.getProperty("retryTimeout"));
        base = new File(properties.getProperty("basePath"));
        snapshotGenerateThreshold = Integer.parseInt(properties.getProperty("snapshotGenerateThreshold"));
        maxTransferLogs = Integer.parseInt(properties.getProperty("maxTransferLogs"));
        maxTransferSize = Integer.parseInt(properties.getProperty("maxTransferSize"));
        linkedBuffPoolSize = Integer.parseInt(properties.getProperty("linkedBuffPoolSize"));
        readIdleTimeout = Integer.parseInt(properties.getProperty("readIdleTimeout"));
        writeIdleTimeout = Integer.parseInt(properties.getProperty("writeIdleTimeout"));
        allIdleTimeout = Integer.parseInt(properties.getProperty("allIdleTimeout"));
        useDirectByteBuffer = Boolean.parseBoolean(properties.getProperty("useDirectByteBuffer"));
        byteBufferPoolSize = Integer.parseInt(properties.getProperty("byteBufferPoolSize"));
        byteBufferSizeLimit = Integer.parseInt(properties.getProperty("byteBufferSizeLimit"));
        synLogFlush = Boolean.parseBoolean(properties.getProperty("synLogFlush"));
        blockCacheSize = Integer.parseInt(properties.getProperty("blockCacheSize"));
        blockSize = Integer.parseInt(properties.getProperty("blockSize"));
        blockFlushInterval = Long.parseLong(properties.getProperty("blockFlushInterval"));
        logIndexCacheSize = Integer.parseInt(properties.getProperty("logIndexCacheSize"));
        lazyFlushInterval = Long.parseLong(properties.getProperty("lazyFlushInterval"));
        lazyFlushThreshold = Double.parseDouble(properties.getProperty("lazyFlushThreshold"));
    }

}
