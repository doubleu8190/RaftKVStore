package cn.ttplatform.wh;

import cn.ttplatform.wh.support.ResponseFuture;
import java.io.Closeable;
import java.util.concurrent.TimeUnit;

/**
 * Raft KV Store client interface.
 *
 * Supports both blocking synchronous and async callback execution modes:
 * <pre>{@code
 *   // Async mode with callback
 *   client.set("key", "value").thenAccept(success -> {
 *       if (success) System.out.println("Set succeeded!");
 *   });
 *
 *   // Sync (blocking) mode
 *   boolean success = client.setSync("key", "value", 3, TimeUnit.SECONDS);
 * }</pre>
 *
 * @author Wang Hao
 * @date 2021/5/26 21:25
 */
public interface RaftKVClient extends Closeable {

    // ---- Connection lifecycle ----

    /**
     * Connect to the configured host and port.
     */
    void connect();

    /**
     * Connect to the given host and port.
     */
    void connect(String host, int port);

    /**
     * Check whether the client is currently connected.
     */
    boolean isConnected();

    /**
     * Close the connection and release resources.
     */
    @Override
    void close();

    // ---- Set operations ----

    /**
     * Set a key-value pair (async).
     *
     * @param key   the key
     * @param value the value
     * @return a future containing {@code true} on success
     */
    ResponseFuture<Boolean> set(String key, String value);

    /**
     * Set a key-value pair (blocking).
     *
     * @param key     the key
     * @param value   the value
     * @param timeout the max time to wait
     * @param unit    the time unit
     * @return {@code true} on success
     * @throws cn.ttplatform.wh.support.ClientException on timeout or server error
     */
    boolean setSync(String key, String value, long timeout, TimeUnit unit);

    // ---- Get operations ----

    /**
     * Get a value by key (async).
     *
     * @param key the key
     * @return a future containing the value, or {@code null} if not found
     */
    ResponseFuture<String> get(String key);

    /**
     * Get a value by key (blocking).
     *
     * @param key     the key
     * @param timeout the max time to wait
     * @param unit    the time unit
     * @return the value, or {@code null} if not found
     * @throws cn.ttplatform.wh.support.ClientException on timeout or server error
     */
    String getSync(String key, long timeout, TimeUnit unit);

    // ---- Cluster info ----

    /**
     * Get cluster information (async).
     *
     * @return a future containing the cluster info
     */
    ResponseFuture<ClusterInfo> getClusterInfo();

    /**
     * Get cluster information (blocking).
     *
     * @param timeout the max time to wait
     * @param unit    the time unit
     * @return the cluster info
     * @throws cn.ttplatform.wh.support.ClientException on timeout or server error
     */
    ClusterInfo getClusterInfoSync(long timeout, TimeUnit unit);

    // ---- Cluster membership ----

    /**
     * Add a new node to the cluster (async).
     *
     * @param nodeId        the new node's ID
     * @param host          the new node's host
     * @param port          the new node's client port
     * @param connectorPort the new node's raft connector port
     * @return a future containing {@code true} on success
     */
    ResponseFuture<Boolean> addNode(String nodeId, String host, int port, int connectorPort);

    /**
     * Add a new node to the cluster (blocking).
     *
     * @param nodeId        the new node's ID
     * @param host          the new node's host
     * @param port          the new node's client port
     * @param connectorPort the new node's raft connector port
     * @param timeout       the max time to wait
     * @param unit          the time unit
     * @return {@code true} on success
     * @throws cn.ttplatform.wh.support.ClientException on timeout or server error
     */
    boolean addNodeSync(String nodeId, String host, int port, int connectorPort,
                        long timeout, TimeUnit unit);

    /**
     * Remove a node from the cluster (async).
     *
     * @param nodeId the node ID to remove
     * @return a future containing {@code true} on success
     */
    ResponseFuture<Boolean> removeNode(String nodeId);

    /**
     * Remove a node from the cluster (blocking).
     *
     * @param nodeId  the node ID to remove
     * @param timeout the max time to wait
     * @param unit    the time unit
     * @return {@code true} on success
     * @throws cn.ttplatform.wh.support.ClientException on timeout or server error
     */
    boolean removeNodeSync(String nodeId, long timeout, TimeUnit unit);
}
