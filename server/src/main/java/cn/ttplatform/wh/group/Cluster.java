package cn.ttplatform.wh.group;

import cn.ttplatform.wh.GlobalContext;
import cn.ttplatform.wh.config.ServerProperties;
import cn.ttplatform.wh.support.Pool;
import io.protostuff.LinkedBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Wang Hao
 * @date 2020/6/30 下午9:46
 */
@Slf4j
public class Cluster {

    private final String selfId;
    private final NewConfigSerializer newConfigFactory;
    private final OldNewConfigSerializer oldNewConfigFactory;
    private final Map<String, Endpoint> endpointMap;
    @Setter
    @Getter
    private Map<String, Endpoint> newConfigMap;
    @Setter
    @Getter
    private Phase phase;
    @Setter
    @Getter
    private int logSynCompleteState;

    public Cluster(GlobalContext context) {
        Set<Endpoint> endpoints = initClusterEndpoints(context.getProperties());
        this.endpointMap = buildMap(endpoints);
        this.newConfigMap = new HashMap<>();
        this.selfId = context.getProperties().getNodeId();
        this.phase = Phase.STABLE;
        Pool<LinkedBuffer> linkedBufferPool = context.getLinkedBufferPool();
        this.newConfigFactory = new NewConfigSerializer(linkedBufferPool);
        this.oldNewConfigFactory = new OldNewConfigSerializer(linkedBufferPool);
    }

    private Set<Endpoint> initClusterEndpoints(ServerProperties properties) {
        String clusterInfo = properties.getClusterInfo();
        if (clusterInfo == null || "".equals(clusterInfo)) {
            return Collections.emptySet();
        }
        return Arrays.stream(clusterInfo.split(" ")).map(Endpoint::new).collect(Collectors.toSet());
    }

    /**
     * When a node becomes the new leader, this method must be executed to reset the log replication status of the follower
     *
     * @param initLeftEdge  left edge
     * @param initRightEdge right edge
     */
    public void resetReplicationStates(int initLeftEdge, int initRightEdge) {
        endpointMap.forEach((id, endpoint) -> endpoint.resetReplicationState(initLeftEdge, initRightEdge));
        if (phase != Phase.STABLE) {
            newConfigMap.forEach((id, endpoint) -> endpoint.resetReplicationState(initLeftEdge, initRightEdge));
        }
    }

    private Map<String, Endpoint> buildMap(Collection<Endpoint> endpoints) {
        Map<String, Endpoint> map = new HashMap<>((int) (endpoints.size() / 0.75f + 1));
        endpoints.forEach(endpoint -> map.put(endpoint.getNodeId(), endpoint));
        return map;
    }

    public Endpoint find(String nodeId) {
        if (phase == Phase.STABLE) {
            return endpointMap.get(nodeId);
        }
        Endpoint endpoint = endpointMap.get(nodeId);
        return endpoint == null ? newConfigMap.get(nodeId) : endpoint;
    }

    public Endpoint findFromOldConfig(String nodeId) {
        return endpointMap.get(nodeId);
    }

    public String getOldConfigStr() {
        return endpointMap.toString();
    }

    public String getNewConfigStr() {
        return newConfigMap.toString();
    }

    public int getOldConfigSize() {
        return endpointMap.size();
    }

    public int getNewConfigSize() {
        return newConfigMap.size();
    }

    public boolean inNewConfig(String nodeId) {
        return newConfigMap.containsKey(nodeId);
    }

    public boolean inOldConfig(String nodeId) {
        return endpointMap.containsKey(nodeId);
    }

    public void exchangeConfig() {
        endpointMap.clear();
        if (inNewConfig(selfId)) {
            endpointMap.putAll(newConfigMap);
        }
        newConfigMap.clear();
    }

    public boolean syncingCompleted() {
        for (Entry<String, Endpoint> entry : newConfigMap.entrySet()) {
            if (!selfId.equals(entry.getKey()) && entry.getValue().getMatchIndex() < logSynCompleteState) {
                log.info("log syn is uncompleted, node is {}, logSynCompleteState is {}.", entry, logSynCompleteState);
                return false;
            }
        }
        return true;
    }

    public List<Endpoint> getAllEndpointExceptSelf() {
        List<Endpoint> result = new ArrayList<>();
        endpointMap.forEach((id, endpoint) -> {
            if (!selfId.equals(id)) {
                result.add(endpoint);
            }
        });
        if (phase != Phase.STABLE) {
            newConfigMap.forEach((id, endpoint) -> {
                if (!selfId.equals(id) && !endpointMap.containsKey(id)) {
                    result.add(endpoint);
                }
            });
        }
        return result;
    }

    public List<EndpointMetaData> getAllEndpointMetaData() {
        List<EndpointMetaData> result = new ArrayList<>();
        endpointMap.forEach((id, endpoint) -> result.add(endpoint.getMetaData()));
        return result;
    }

    public int getNewCommitIndexFromNewConfig() {
        return getNewCommitIndexFrom(newConfigMap);
    }

    public int getNewCommitIndexFromOldConfig() {
        return getNewCommitIndexFrom(endpointMap);
    }

    public int getNewCommitIndexFrom(Map<String, Endpoint> endpointMap) {
        List<Endpoint> endpoints = new ArrayList<>(endpointMap.size());
        endpointMap.forEach((id, endpoint) -> {
            if (!selfId.equals(id)) {
                endpoints.add(endpoint);
            }
        });
        int size = endpoints.size();
        // After sorting from small to large according to the matchIndex, take
        // the matchIndex of the left node of the middle node to be the new commitIndex
        Collections.sort(endpoints);
        return endpoints.get(size >> 1).getMatchIndex();
    }


    public OldNewConfig createOldNewConfig(byte[] config) {
        return oldNewConfigFactory.deserialize(config, config.length);
    }

    public NewConfig createNewConfig(byte[] config) {
        return newConfigFactory.deserialize(config, config.length);
    }

    public byte[] getOldNewConfigBytes() {
        Set<EndpointMetaData> oldConfigs = new HashSet<>(endpointMap.size());
        endpointMap.forEach((id, endpoint) -> oldConfigs.add(endpoint.getMetaData()));
        Set<EndpointMetaData> newConfigs = new HashSet<>(newConfigMap.size());
        newConfigMap.forEach((id, endpoint) -> newConfigs.add(endpoint.getMetaData()));
        OldNewConfig oldNewConfig = new OldNewConfig(oldConfigs, newConfigs);
        return oldNewConfigFactory.serialize(oldNewConfig);
    }

    public byte[] getNewConfigBytes() {
        List<EndpointMetaData> newConfigs = new ArrayList<>(newConfigMap.size());
        newConfigMap.forEach((id, endpoint) -> newConfigs.add(endpoint.getMetaData()));
        NewConfig newConfig = new NewConfig(newConfigs);
        return newConfigFactory.serialize(newConfig);
    }

}

