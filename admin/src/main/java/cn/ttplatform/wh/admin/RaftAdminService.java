package cn.ttplatform.wh.admin;

import cn.ttplatform.wh.ClusterInfo;
import cn.ttplatform.wh.RaftKVClient;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * Service layer wrapping {@link RaftKVClient} for the admin web UI.
 *
 * Each method calls the blocking sync variant and wraps results in a
 * {@link ServiceResult}, catching exceptions and converting them to
 * error results.
 *
 * @author Wang Hao
 * @date 2021/5/26 21:25
 */
@Slf4j
public class RaftAdminService {

    private static final long DEFAULT_TIMEOUT = 5;
    private static final TimeUnit DEFAULT_TIMEOUT_UNIT = TimeUnit.SECONDS;

    private final RaftKVClient client;

    public RaftAdminService(RaftKVClient client) {
        this.client = client;
    }

    public boolean isConnected() {
        return client.isConnected();
    }

    // ---- Set ----

    public ServiceResult set(String key, String value) {
        try {
            boolean ok = client.setSync(key, value, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT);
            log.info("SET '{}' = '{}' -> {}", key, value, ok);
            return ServiceResult.ok(ok);
        } catch (Exception e) {
            log.error("SET '{}' failed: {}", key, e.getMessage());
            return ServiceResult.error(e.getMessage());
        }
    }

    // ---- Get ----

    public ServiceResult get(String key) {
        try {
            String value = client.getSync(key, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT);
            log.info("GET '{}' -> {}", key, value);
            return ServiceResult.ok(value);
        } catch (Exception e) {
            log.error("GET '{}' failed: {}", key, e.getMessage());
            return ServiceResult.error(e.getMessage());
        }
    }

    // ---- Cluster Info ----

    public ServiceResult getClusterInfo() {
        try {
            ClusterInfo info = client.getClusterInfoSync(DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT);
            log.info("ClusterInfo: leader={}, phase={}, mode={}, size={}",
                info.getLeader(), info.getPhase(), info.getMode(), info.getSize());
            return ServiceResult.ok(info);
        } catch (Exception e) {
            log.error("getClusterInfo failed: {}", e.getMessage());
            return ServiceResult.error(e.getMessage());
        }
    }

    // ---- Add Node ----

    public ServiceResult addNode(String nodeId, String host, int port, int connectorPort) {
        try {
            boolean ok = client.addNodeSync(nodeId, host, port, connectorPort,
                DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT);
            log.info("AddNode {} ({}:{}:{}) -> {}", nodeId, host, port, connectorPort, ok);
            return ServiceResult.ok(ok);
        } catch (Exception e) {
            log.error("AddNode {} failed: {}", nodeId, e.getMessage());
            return ServiceResult.error(e.getMessage());
        }
    }

    // ---- Remove Node ----

    public ServiceResult removeNode(String nodeId) {
        try {
            boolean ok = client.removeNodeSync(nodeId, DEFAULT_TIMEOUT, DEFAULT_TIMEOUT_UNIT);
            log.info("RemoveNode {} -> {}", nodeId, ok);
            return ServiceResult.ok(ok);
        } catch (Exception e) {
            log.error("RemoveNode {} failed: {}", nodeId, e.getMessage());
            return ServiceResult.error(e.getMessage());
        }
    }

    /**
     * Unified result type for all admin operations.
     */
    public record ServiceResult(boolean success, Object data, String error) {

        public static ServiceResult ok(Object data) {
            return new ServiceResult(true, data, null);
        }

        public static ServiceResult error(String message) {
            return new ServiceResult(false, null, message);
        }
    }
}
