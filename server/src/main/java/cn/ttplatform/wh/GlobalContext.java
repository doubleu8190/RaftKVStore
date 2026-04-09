package cn.ttplatform.wh;

import cn.ttplatform.wh.cmd.ClusterChangeCommand;
import cn.ttplatform.wh.cmd.ClusterChangeResultCommand;
import cn.ttplatform.wh.cmd.GetClusterInfoResultCommand;
import cn.ttplatform.wh.cmd.GetCommand;
import cn.ttplatform.wh.cmd.GetResultCommand;
import cn.ttplatform.wh.cmd.KeyValuePair;
import cn.ttplatform.wh.cmd.SetCommand;
import cn.ttplatform.wh.cmd.SetResultCommand;
import cn.ttplatform.wh.cmd.factory.ClusterChangeCommandSerializer;
import cn.ttplatform.wh.cmd.factory.ClusterChangeResultCommandSerializer;
import cn.ttplatform.wh.cmd.factory.GetClusterInfoCommandSerializer;
import cn.ttplatform.wh.cmd.factory.GetClusterInfoResultCommandSerializer;
import cn.ttplatform.wh.cmd.factory.GetCommandSerializer;
import cn.ttplatform.wh.cmd.factory.GetResultCommandSerializer;
import cn.ttplatform.wh.cmd.factory.KVEntrySerializer;
import cn.ttplatform.wh.cmd.factory.RedirectCommandSerializer;
import cn.ttplatform.wh.cmd.factory.RequestFailedCommandSerializer;
import cn.ttplatform.wh.cmd.factory.SetCommandSerializer;
import cn.ttplatform.wh.cmd.factory.SetResultCommandSerializer;
import cn.ttplatform.wh.config.RunMode;
import cn.ttplatform.wh.config.ServerProperties;
import cn.ttplatform.wh.constant.ErrorMessage;
import cn.ttplatform.wh.data.DataManager;
import cn.ttplatform.wh.data.log.Log;
import cn.ttplatform.wh.data.log.LogFactory;
import cn.ttplatform.wh.data.snapshot.GenerateSnapshotTask;
import cn.ttplatform.wh.group.Cluster;
import cn.ttplatform.wh.group.Connector;
import cn.ttplatform.wh.group.Endpoint;
import cn.ttplatform.wh.group.EndpointMetaData;
import cn.ttplatform.wh.group.NewConfig;
import cn.ttplatform.wh.group.OldNewConfig;
import cn.ttplatform.wh.group.Phase;
import cn.ttplatform.wh.handler.ClusterChangeCommandHandler;
import cn.ttplatform.wh.handler.GetClusterInfoCommandHandler;
import cn.ttplatform.wh.handler.GetCommandHandler;
import cn.ttplatform.wh.handler.SetCommandHandler;
import cn.ttplatform.wh.message.PreVoteMessage;
import cn.ttplatform.wh.message.RequestVoteMessage;
import cn.ttplatform.wh.handler.SyncingCommand;
import cn.ttplatform.wh.message.handler.AppendLogEntriesMessageHandler;
import cn.ttplatform.wh.message.handler.AppendLogEntriesResultMessageHandler;
import cn.ttplatform.wh.message.handler.InstallSnapshotMessageHandler;
import cn.ttplatform.wh.message.handler.InstallSnapshotResultMessageHandler;
import cn.ttplatform.wh.message.handler.PreVoteMessageHandler;
import cn.ttplatform.wh.message.handler.PreVoteResultMessageHandler;
import cn.ttplatform.wh.message.handler.RequestVoteMessageHandler;
import cn.ttplatform.wh.message.handler.RequestVoteResultMessageHandler;
import cn.ttplatform.wh.handler.SyncingCommandHandler;
import cn.ttplatform.wh.message.serializer.AppendLogEntriesMessageSerializer;
import cn.ttplatform.wh.message.serializer.AppendLogEntriesResultMessageSerializer;
import cn.ttplatform.wh.message.serializer.InstallSnapshotMessageSerializer;
import cn.ttplatform.wh.message.serializer.InstallSnapshotResultMessageSerializer;
import cn.ttplatform.wh.message.serializer.PreVoteMessageSerializer;
import cn.ttplatform.wh.message.serializer.PreVoteResultMessageSerializer;
import cn.ttplatform.wh.message.serializer.RequestVoteMessageSerializer;
import cn.ttplatform.wh.message.serializer.RequestVoteResultMessageSerializer;
import cn.ttplatform.wh.message.serializer.SyncingMessageSerializer;
import cn.ttplatform.wh.scheduler.Scheduler;
import cn.ttplatform.wh.scheduler.SingleThreadScheduler;
import cn.ttplatform.wh.support.ChannelPool;
import cn.ttplatform.wh.support.CommonDistributor;
import cn.ttplatform.wh.support.DirectByteBufferPool;
import cn.ttplatform.wh.support.DistributableSerializerRegistry;
import cn.ttplatform.wh.support.FixedSizeLinkedBufferPool;
import cn.ttplatform.wh.support.HeapByteBufferPool;
import cn.ttplatform.wh.support.Message;
import cn.ttplatform.wh.support.NamedThreadFactory;
import cn.ttplatform.wh.support.Pool;
import io.netty.channel.ChannelFuture;
import io.netty.channel.nio.NioEventLoopGroup;
import io.protostuff.LinkedBuffer;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Wang Hao
 * @date 2020/6/30 下午9:39
 */
@Setter
@Getter
@Builder
@AllArgsConstructor
public class GlobalContext {

    private final Logger logger = LoggerFactory.getLogger(GlobalContext.class);
    private final Map<Integer, List<GetCommand>> pendingGetCommandMap = new HashMap<>();
    private final Map<Integer, SetCommand> pendingSetCommandMap = new HashMap<>();
    private final ServerProperties properties;
    private final Pool<LinkedBuffer> linkedBufferPool;
    private final Pool<ByteBuffer> byteBufferPool;
    private final CommonDistributor distributor;
    private final DistributableSerializerRegistry serializerRegistry;
    private final ThreadPoolExecutor subTaskExecutor;
    private final ThreadPoolExecutor executor;
    private final NioEventLoopGroup boss;
    private final NioEventLoopGroup worker;
    private final StateMachine stateMachine;
    private final DataManager dataManager;
    private final KVEntrySerializer kvEntrySerializer;
    private final ChannelPool channelPool;
    private Node node;
    private Scheduler scheduler;
    private Cluster cluster;
    private Connector connector;
    private ClusterChangeCommand clusterChangeCommand;
    private boolean clusterEnabled;

    public GlobalContext(Node node) {
        this.node = node;
        this.properties = node.getProperties();
        this.linkedBufferPool = new FixedSizeLinkedBufferPool(properties.getLinkedBuffPoolSize());
        if (properties.isUseDirectByteBuffer()) {
            logger.debug("use DirectBufferAllocator");
            this.byteBufferPool = new DirectByteBufferPool(properties.getByteBufferPoolSize(),
                    properties.getBlockSize(), properties.getByteBufferSizeLimit());
        } else {
            logger.debug("use BufferAllocator");
            this.byteBufferPool = new HeapByteBufferPool(properties.getByteBufferPoolSize(),
                    properties.getBlockSize(), properties.getByteBufferSizeLimit());
        }
        this.distributor = buildDistributor();
        this.serializerRegistry = buildSerializerRegistry();
        this.executor = new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                new NamedThreadFactory("core-"));
        this.subTaskExecutor = new ThreadPoolExecutor(
                0,
                1,
                30L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(2),
                new NamedThreadFactory("subTask-"),
                (r, e) -> logger.error("There is currently an executing task, reject this operation."));
        this.boss = new NioEventLoopGroup(properties.getBossThreads(), new NamedThreadFactory("boss-"));
        this.worker = new NioEventLoopGroup(properties.getWorkerThreads(), new NamedThreadFactory("worker-"));
        this.stateMachine = new StateMachine(this);
        this.dataManager = new DataManager(this);
        this.kvEntrySerializer = new KVEntrySerializer(this.linkedBufferPool);
        this.channelPool = new ChannelPool();
    }

    public void enableClusterComponent() {
        if (!clusterEnabled) {
            clusterEnabled = true;
            logger.info("enable cluster component...");
            this.connector = new Connector(this);
            this.connector.listen(new InetSocketAddress(properties.getConnectorHost(), properties.getConnectorPort()));
            this.scheduler = new SingleThreadScheduler(properties, executor);
            this.cluster = new Cluster(this);
        }
    }

    public void enterClusterMode() {
        node.setMode(RunMode.CLUSTER);
        enableClusterComponent();
    }

    private CommonDistributor buildDistributor() {
        CommonDistributor commonDistributor = new CommonDistributor();
        commonDistributor.register(new GetClusterInfoCommandHandler(this));
        commonDistributor.register(new ClusterChangeCommandHandler(this));
        commonDistributor.register(new SetCommandHandler(this));
        commonDistributor.register(new GetCommandHandler(this));
        commonDistributor.register(new AppendLogEntriesMessageHandler(this));
        commonDistributor.register(new AppendLogEntriesResultMessageHandler(this));
        commonDistributor.register(new RequestVoteMessageHandler(this));
        commonDistributor.register(new RequestVoteResultMessageHandler(this));
        commonDistributor.register(new PreVoteMessageHandler(this));
        commonDistributor.register(new PreVoteResultMessageHandler(this));
        commonDistributor.register(new InstallSnapshotMessageHandler(this));
        commonDistributor.register(new InstallSnapshotResultMessageHandler(this));
        commonDistributor.register(new SyncingCommandHandler(this));
        return commonDistributor;
    }


    private DistributableSerializerRegistry buildSerializerRegistry() {
        DistributableSerializerRegistry registry = new DistributableSerializerRegistry();
        registry.register(new SetCommandSerializer(linkedBufferPool));
        registry.register(new SetResultCommandSerializer(linkedBufferPool));
        registry.register(new GetCommandSerializer(linkedBufferPool));
        registry.register(new GetResultCommandSerializer(linkedBufferPool));
        registry.register(new RedirectCommandSerializer(linkedBufferPool));
        registry.register(new ClusterChangeCommandSerializer(linkedBufferPool));
        registry.register(new ClusterChangeResultCommandSerializer(linkedBufferPool));
        registry.register(new RequestFailedCommandSerializer(linkedBufferPool));
        registry.register(new GetClusterInfoCommandSerializer(linkedBufferPool));
        registry.register(new GetClusterInfoResultCommandSerializer(linkedBufferPool));
        registry.register(new AppendLogEntriesMessageSerializer(linkedBufferPool));
        registry.register(new AppendLogEntriesResultMessageSerializer(linkedBufferPool));
        registry.register(new RequestVoteMessageSerializer(linkedBufferPool));
        registry.register(new RequestVoteResultMessageSerializer(linkedBufferPool));
        registry.register(new PreVoteMessageSerializer(linkedBufferPool));
        registry.register(new PreVoteResultMessageSerializer(linkedBufferPool));
        registry.register(new InstallSnapshotMessageSerializer(linkedBufferPool));
        registry.register(new InstallSnapshotResultMessageSerializer(linkedBufferPool));
        registry.register(new SyncingMessageSerializer(linkedBufferPool));
        return registry;
    }

    public ScheduledFuture<?> electionTimeoutTask() {
        return scheduler.scheduleElectionTimeoutTask(() -> {
            if (node.isLeader()) {
                logger.warn("current node[{}] role type is leader, ignore this process.", properties.getNodeId());
                return;
            }
            int currentTerm = node.getTerm();
            if (node.isCandidate()) {
                startElection(currentTerm + 1);
            } else {
                String selfId = node.getSelfId();
                int oldCounts = cluster.inOldConfig(selfId) ? 1 : 0;
                int newCounts = cluster.inNewConfig(selfId) ? 1 : 0;
                node.changeToFollower(currentTerm, null, null, oldCounts, newCounts, 0L);
                PreVoteMessage preVoteMessage = PreVoteMessage.builder()
                        .lastLogTerm(dataManager.getTermOfLastLog())
                        .lastLogIndex(dataManager.getIndexOfLastLog())
                        .build();
                sendMessageToOthers(preVoteMessage);
            }
        });
    }

    public void startElection(int term) {
        logger.debug("startElection in term[{}].", term);
        String selfId = node.getSelfId();
        node.changeToCandidate(term, cluster.inOldConfig(selfId) ? 1 : 0, cluster.inOldConfig(selfId) ? 1 : 0);
        RequestVoteMessage requestVoteMessage = RequestVoteMessage.builder()
                .lastLogIndex(dataManager.getIndexOfLastLog())
                .lastLogTerm(dataManager.getTermOfLastLog())
                .term(term)
                .build();
        sendMessageToOthers(requestVoteMessage);
    }

    public ScheduledFuture<?> logReplicationTask(boolean newThreadExec) {
        if (newThreadExec) {
            return scheduler.scheduleLogReplicationTask(this::doLogReplication);
        }
        doLogReplication();
        return null;
    }

    private void doLogReplication() {
        int currentTerm = node.getTerm();
        cluster.getAllEndpointExceptSelf().forEach(endpoint -> {
            // If the log is not being transmitted, the heartbeat detection information will be sent every time
            // the scheduled task is executed, otherwise the message will be sent only when a certain time has passed.
            // Doing so will cause a problem that the results of the slave processing log snapshot messages may not be
            // returned in time, causing the master to resend the last message because it does not receive a reply. If
            // the slave does not handle it, an unknown error will occur.
            if (!endpoint.isReplicating() || System.currentTimeMillis() - endpoint.getLastHeartBeat() >= properties
                    .getMinElectionTimeout()) {
                doLogReplication(endpoint, currentTerm);
            }
        });
    }

    public void doLogReplication(Endpoint endpoint, int currentTerm) {
        Message message = dataManager.createAppendLogEntriesMessage(currentTerm, endpoint, properties.getMaxTransferLogs());
        if (message == null) {
            // start snapshot replication
            message = dataManager
                    .createInstallSnapshotMessage(currentTerm, endpoint.getSnapshotOffset(), properties.getMaxTransferSize());
        }
        sendMessage(message, endpoint);
        endpoint.setReplicating(true);
        endpoint.setLastHeartBeat(System.currentTimeMillis());
    }

    public void sendMessageToOthers(Message message) {
        cluster.getAllEndpointExceptSelf().forEach(endpoint -> sendMessage(message, endpoint));
    }

    public void sendMessage(Message message, String nodeId) {
        sendMessage(message, cluster.find(nodeId));
    }

    public void sendMessage(Message message, Endpoint endpoint) {
        message.setSourceId(node.getSelfId());
        connector.send(message, endpoint.getMetaData());
    }

    public void setProperty(String fieldName, Object value) {
        Field field;
        try {
            field = properties.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(properties, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.error("fail to set property[filed={}, value={}], error detail is {}.", fieldName, value, e.getStackTrace());
        }
    }

    public Object getProperty(String fieldName) {
        Field field;
        try {
            field = properties.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return field.get(properties);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            logger.error("fail to get property[filed={}], error detail is {}.", fieldName, e.getStackTrace());
            return null;
        }
    }

    public void advanceLastApplied(int newCommitIndex) {
        int applied = stateMachine.getApplied();
        int lastIncludeIndex = dataManager.getLastIncludeIndex();
        if (applied == 0 && lastIncludeIndex > 0) {
            logger.debug("lastApplied is 0, and there is a none empty snapshot, then apply snapshot.");
            ByteBuffer byteBuffer = dataManager.getSnapshotData();
            try {
                stateMachine.applySnapshotData(byteBuffer, lastIncludeIndex);
            } finally {
                byteBufferPool.recycle(byteBuffer);
            }
            applied = lastIncludeIndex;
        }
        applyLogs(dataManager.range(applied + 1, newCommitIndex + 1));
        logger.info("advanceLastApplied, newCommitIndex is {}.", newCommitIndex);
        if (dataManager.shouldGenerateSnapshot(properties.getSnapshotGenerateThreshold())) {
            boolean result = stateMachine.startGenerating();
            if (result) {
                subTaskExecutor.execute(new GenerateSnapshotTask(this, stateMachine.getApplied()));
            } else {
                // Perhaps the threshold for log snapshot generation should be appropriately increased
                logger.debug("There is currently an executing task, reject this operation.");
            }
        }
    }


    public boolean canAdvanceCommitIndex(int newCommitIndex, int term) {

        if (newCommitIndex <= dataManager.getCommitIndex()) {
            logger.debug("newCommitIndex[{}]<=commitIndex[{}], can not advance commitIndex", newCommitIndex, dataManager.getCommitIndex());
            return false;
        }
        Log log = dataManager.getLog(newCommitIndex);
        if (log == null) {
            logger.debug("not found a log for index[{}].", newCommitIndex);
            return false;
        }
        /*
            Raft 有一条强约束：
            1. Leader 不能主动提交任何不属于自己当前任期的日志条目。
            只能提交当前 Term 自己产生的日志。
            2. 但往期日志会被 “顺带提交”
            当 Leader 提交一条当前任期的日志时，
            Raft 会根据日志匹配特性，把这条日志之前所有已经复制到多数节点的旧任期日志，一并认定为 committed。
            3. 为什么要这么设计？
            为了保证 Leader Completeness 特性（Leader 完整性）：
            如果允许直接提交旧 Term 日志，可能出现旧日志被提交后又被覆盖的情况；
            只有通过当前 Term 日志 “带提交”，才能保证已提交的日志永远不会丢失、不会被回滚。
         */
        if (node.isLeader() && log.getTerm() != term) {
            logger.debug("Log[{}] is not term[{}], unmatched.", log, term);
            return false;
        }
        return true;
    }

    public void applySnapshot(int lastIncludeIndex) {
        ByteBuffer byteBuffer = dataManager.getSnapshotData();
        stateMachine.applySnapshotData(byteBuffer, lastIncludeIndex);
        byteBufferPool.recycle(byteBuffer);
    }

    public void applyLogs(List<Log> logs) {
        Optional.ofNullable(logs).orElse(Collections.emptyList()).forEach(logEntry -> {
            if (node.isLeader() && node.getMode() == RunMode.CLUSTER) {
                if (logEntry.getType() == Log.OLD_NEW) {
                    // At this point, the leader has persisted the Coldnew log to the disk
                    // file, and then needs to send a Cnew log and then enter the NEW phase
                    logger.info("OLD_NEW log had been committed");
                    enterNewPhase();
                } else if (logEntry.getType() == Log.NEW) {
                    // At this point, Cnew Log had been committed, then enter STABLE phase, if the node
                    // is not exist in new config, the then node will go offline.
                    logger.info("NEW log had been committed");
                    enterStablePhase();
                }
            }
            applyLog(logEntry);
        });
    }

    /**
     * Calculate the new commitIndex based on the current phase of the cluster: 1. If the cluster is in the STABLE phase, only the
     * majority of nodes in oldConfig need to agree to submit the log. 2. If the cluster is in the NEW phase, only the majority of
     * nodes in newConfig need to agree to submit the log. 3. If the cluster is in the OLD_NEW phase, you need to agree to the
     * majority of nodes in newConfig and oldConfig before you can submit the log
     *
     * @return index needed to be committed
     */
    public int getNewCommitIndex() {
        Phase phase = currentPhase();
        if (phase == Phase.OLD_NEW) {
            int oldConfigCommitIndex =
                    cluster.getOldConfigSize() <= 1 ? dataManager.getNextIndex() - 1 : cluster.getNewCommitIndexFromOldConfig();
            int newConfigCommitIndex = cluster.getNewCommitIndexFromNewConfig();
            logger.debug("oldConfigCommitIndex is {}.", oldConfigCommitIndex);
            logger.debug("newConfigCommitIndex is {}.", newConfigCommitIndex);
            return Math.min(oldConfigCommitIndex, newConfigCommitIndex);
        }
        if (phase == Phase.NEW) {
            int newConfigCommitIndex = cluster.getNewCommitIndexFromNewConfig();
            logger.debug("newConfigCommitIndex is {}.", newConfigCommitIndex);
            return newConfigCommitIndex;
        }
        int oldConfigCommitIndex =
                cluster.getOldConfigSize() <= 1 ? dataManager.getNextIndex() - 1 : cluster.getNewCommitIndexFromOldConfig();
        logger.debug("oldConfigCommitIndex is {}.", oldConfigCommitIndex);
        return oldConfigCommitIndex;
    }

    public Endpoint getEndpoint(String nodeId) {
        return cluster.find(nodeId);
    }

    public boolean syncingPhaseCompleted(String nodeId) {
        return Phase.SYNCING == cluster.getPhase() && isSyncingNode(nodeId) && synHasComplete();
    }

    public boolean isSyncingNode(String nodeId) {
        Phase phase = currentPhase();
        if (phase != Phase.SYNCING) {
            throw new UnsupportedOperationException(String.format(ErrorMessage.NOT_SYNCING_PHASE, phase));
        }
        boolean res = cluster.inNewConfig(nodeId) && !cluster.inOldConfig(nodeId);
        if (res) {
            logger.info("{} is syncing node", nodeId);
        }
        return res;
    }

    /**
     * The SYNCING phase is added based on the original joint consensus. The task of this phase is to synchronize the logs of the
     * newly added nodes. Only after the synchronization is completed can the OLD_NEW phase be entered. Therefore, this phase can
     * be skipped directly if there is no new node. Only when all the newly added nodes have copied the expected set log (index =
     * logSynCompleteState) is the synchronization completed.
     *
     * @return Is it done
     */
    public boolean synHasComplete() {
        Phase phase = currentPhase();
        if (phase != Phase.SYNCING) {
            throw new UnsupportedOperationException(String.format(ErrorMessage.NOT_SYNCING_PHASE, phase));
        }
        return cluster.syncingCompleted();
    }

    public Phase currentPhase() {
        return cluster.getPhase();
    }

    public void enterSyncingPhase() {
        Phase phase = currentPhase();
        if (phase != Phase.STABLE) {
            logger.warn("current phase[{}] is not STABLE.", phase);
            return;
        }
        int logSynCompleteState = getNewCommitIndex();
        cluster.setLogSynCompleteState(logSynCompleteState);
        logger.info("logSynCompleteState is {}", logSynCompleteState);
        cluster.setPhase(Phase.SYNCING);
        logger.info("enter SYNCING phase");

        cluster.getAllEndpointExceptSelf().parallelStream().forEach(endpoint -> {
            if (cluster.inNewConfig(endpoint.getNodeId())) {
                SyncingCommand syncingCommand = SyncingCommand.builder().id(UUID.randomUUID().toString())
                        .leaderMetaData(cluster.find(node.getSelfId()).getMetaData())
                        .followerMetaData(endpoint.getMetaData())
                        .term(node.getTerm()).build();
                connector.send(syncingCommand, endpoint.getMetaData());
            }
        });
    }

    /**
     * All newly added nodes have synchronized logs to the specified state. Begin to enter the OLD_NEW phase
     */
    public void enterOldNewPhase() {
        Phase phase = currentPhase();
        if (phase != Phase.STABLE && phase != Phase.SYNCING) {
            logger.warn("current phase[{}] is not STABLE or SYNCING.", phase);
            return;
        }
        if (node.isLeader()) {
            pendingLog(Log.OLD_NEW, cluster.getOldNewConfigBytes());
            logger.info("pending OLD_NEW log");
        }
        cluster.setPhase(Phase.OLD_NEW);
        logger.info("enter OLD_NEW phase");
    }

    /**
     * Leader and follower enter the NEW phase at different times. The follower enters the NEW phase after receiving the NEW log
     * from the leader, and the leader enters the NEW phase after submitting the OLD_NEW log. Moreover, after the leader enters
     * the NEW phase, if it finds that it is not in the newConfig, it will not exit the cluster directly, but needs to wait for
     * the NEW log to be submitted before exiting the cluster, but the follower will exit the cluster after receiving the NEW log
     * and replying to the leader.
     */
    public void enterNewPhase() {
        Phase phase = currentPhase();
        if (phase != Phase.OLD_NEW) {
            logger.warn("current phase[{}] is not OLD_NEW.", phase);
            return;
        }
        cluster.setPhase(Phase.NEW);
        if (node.isLeader()) {
            logger.info("pending NEW log");
            pendingLog(Log.NEW, cluster.getNewConfigBytes());
        }
        logger.info("enter NEW phase.");
    }

    /**
     * Entering the STABLE stage indicates that the cluster change has been completed, and nodes not in newConfig will actively
     * become followers. And newConfig will replace oldConfig.
     */
    public void enterStablePhase() {
        Phase phase = currentPhase();
        if (phase != Phase.NEW) {
            logger.debug("current phase[{}] is not NEW.", phase);
            return;
        }
        String selfId = node.getSelfId();
        if (!cluster.inNewConfig(selfId)) {
            // 如果当前节点不在newConfigMap中，那么代表当前节点是要下线的节点，那就需要将当前节点转换为没有leader的孤儿follower
            logger.info("node {} is ready to offline", selfId);
            node.changeToFollower(node.getTerm(), null, null, 0, 0, 0L);
        }
        cluster.exchangeConfig();
        cluster.setPhase(Phase.STABLE);
    }

    public void applyOldNewConfig(byte[] config) {
        OldNewConfig oldNewConfig = cluster.createOldNewConfig(config);
        updateNewConfig(oldNewConfig.getNewConfigs());
        enterOldNewPhase();
    }

    public void applyNewConfig(byte[] config) {
        NewConfig newConfig = cluster.createNewConfig(config);
        Map<String, Endpoint> newConfigMap = new HashMap<>();
        newConfig.getNewConfigs()
                .forEach(endpointMetaData -> newConfigMap.put(endpointMetaData.getNodeId(), new Endpoint(endpointMetaData)));
        cluster.setNewConfigMap(newConfigMap);
        logger.debug("apply new config, newConfigMap is {}", newConfigMap);
        enterNewPhase();
    }

    public boolean updateNewConfig(Set<EndpointMetaData> metaData) {
        AtomicInteger count = new AtomicInteger();
        Map<String, Endpoint> newConfigMap = new HashMap<>();
        metaData.forEach(endpointMetaData -> {
            Endpoint endpoint = cluster.findFromOldConfig(endpointMetaData.getNodeId());
            if (endpoint == null) {
                endpoint = new Endpoint(endpointMetaData);
                endpoint.resetReplicationState(dataManager.getLastIncludeIndex(), dataManager.getNextIndex());
                count.getAndIncrement();
            } else {
                endpoint.setMetaData(endpointMetaData);
            }
            newConfigMap.put(endpointMetaData.getNodeId(), endpoint);
        });
        cluster.setNewConfigMap(newConfigMap);
        logger.debug("updateNewConfigMap {}", metaData);
        return count.get() == 0;
    }

    public void applyLog(Log log) {
        if (log.getIndex() <= stateMachine.getApplied()) {
            return;
        }
        int type = log.getType();
        if (type == Log.SET) {
            replySetResult(log);
        } else if (type == Log.NEW) {
            replyClusterChangeResult();
        } else {
            logger.debug("log[{}] can not be applied, skip.", log);
        }
        stateMachine.setApplied(log.getIndex());
    }

    private void replyClusterChangeResult() {
        if (clusterChangeCommand != null) {
            String id = clusterChangeCommand.getId();
            channelPool.reply(id, ClusterChangeResultCommand.builder().id(id).done(true).build());
            clusterChangeCommand = null;
        }
    }

    private void replySetResult(Log log) {
        SetCommand setCmd = pendingSetCommandMap.remove(log.getIndex());
        KeyValuePair keyValuePair;
        if (setCmd == null) {
            byte[] command = log.getCommand();
            keyValuePair = kvEntrySerializer.deserialize(command, command.length);
            stateMachine.set(keyValuePair.getKey(), keyValuePair.getValue());
        } else {
            keyValuePair = setCmd.getKeyValuePair();
            stateMachine.set(keyValuePair.getKey(), keyValuePair.getValue());
            String requestId = setCmd.getId();
            if (requestId != null) {
                ChannelFuture channelFuture = channelPool
                        .reply(requestId, SetResultCommand.builder().id(requestId).result(true).build());
                if (channelFuture != null) {
                    channelFuture.addListener(future -> {
                        if (future.isSuccess()) {
                            replyGetResult(log.getIndex());
                        }
                    });
                }
            }
        }
    }

    private void replyGetResult(int index) {
        Optional.ofNullable(pendingGetCommandMap.remove(index))
                .orElse(Collections.emptyList())
                .forEach(cmd -> channelPool
                        .reply(cmd.getId(), GetResultCommand.builder().id(cmd.getId()).value(stateMachine.get(cmd.getKey())).build()));
    }

    public void replyGetResult(GetCommand cmd) {
        channelPool.reply(cmd.getId(), GetResultCommand.builder().id(cmd.getId()).value(stateMachine.get(cmd.getKey())).build());
    }

    public void replyGetClusterInfoResult(String requestId) {
        RunMode mode = node.getMode();
        GetClusterInfoResultCommand respCommand = GetClusterInfoResultCommand.builder()
                .id(requestId)
                .leader(node.getSelfId())
                .mode(mode.toString())
                .size(stateMachine.getPairs())
                .build();
        if (mode == RunMode.CLUSTER) {
            respCommand.setPhase(cluster.getPhase().toString());
            respCommand.setNewConfig(cluster.getNewConfigStr());
            respCommand.setOldConfig(cluster.getOldConfigStr());
        }
        channelPool.reply(requestId, respCommand);
    }

    public void addGetTasks(int index, GetCommand cmd) {
        if (index > stateMachine.getApplied()) {
            List<GetCommand> getCommands = pendingGetCommandMap.computeIfAbsent(index, k -> new ArrayList<>());
            getCommands.add(cmd);
        } else {
            channelPool
                    .reply(cmd.getId(), GetResultCommand.builder().id(cmd.getId()).value(stateMachine.get(cmd.getKey())).build());
        }
    }

    public boolean setCurrentClusterChangeTask(ClusterChangeCommand command) {
        if (clusterChangeCommand != null) {
            logger.info("clusterChangeCommand is not null");
            return false;
        }
        clusterChangeCommand = command;
        logger.info("update clusterChangeCommand success.");
        return true;
    }

    public void removeClusterChangeTask() {
        clusterChangeCommand = null;
    }

    public int pendingLog(int type, byte[] cmd) {
        return dataManager.pendingLog(LogFactory.createEntry(type, node.getTerm(), 0, cmd));
    }

    public void addPendingCommand(int index, SetCommand cmd) {
        pendingSetCommandMap.put(index, cmd);
    }

    public void close() {
        subTaskExecutor.shutdownNow();
        dataManager.close();
        if (executor != null) {
            executor.shutdown();
        }
        if (scheduler != null) {
            scheduler.close();
        }
    }

}
