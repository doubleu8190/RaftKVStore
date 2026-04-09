package cn.ttplatform.wh.message.handler;

import cn.ttplatform.wh.GlobalContext;
import cn.ttplatform.wh.Node;
import cn.ttplatform.wh.constant.DistributableType;
import cn.ttplatform.wh.group.Endpoint;
import cn.ttplatform.wh.message.AppendLogEntriesResultMessage;
import cn.ttplatform.wh.support.AbstractDistributableHandler;
import cn.ttplatform.wh.support.Distributable;
import cn.ttplatform.wh.support.Message;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Wang Hao
 * @date 2021/2/17 0:41
 */
@Slf4j
public class AppendLogEntriesResultMessageHandler extends AbstractDistributableHandler {

    public AppendLogEntriesResultMessageHandler(GlobalContext context) {
        super(context);
    }

    @Override
    public int getHandlerType() {
        return DistributableType.APPEND_LOG_ENTRIES_RESULT;
    }

    public void preHandle(Message e) {
        if (context.syncingPhaseCompleted(e.getSourceId())) {
            /*
             The leader starts to use the new configuration and the old configuration at the same
             time, and adds a log containing the new and old configuration to the cluster
             */
            log.info("all syncing Endpoint had catchup, enter OLD_NEW phase");
            context.enterOldNewPhase();
        }
    }

    @Override
    public void doHandleInClusterMode(Distributable distributable) {
        AppendLogEntriesResultMessage message = (AppendLogEntriesResultMessage) distributable;
        int term = message.getTerm();
        Node node = context.getNode();
        int currentTerm = node.getTerm();
        if (term < currentTerm) {
            log.info("received an AppendLogEntriesResultMessage[{}], term[{}] < currentTerm[{}], ignore it.", message, term, currentTerm);
            return;
        }
        if (term > currentTerm) {
            log.info("received an AppendLogEntriesResultMessage[{}], term[{}] > currentTerm[{}], become follower.", message, term, currentTerm);
            node.changeToFollower(term, null, null, 0, 0, 0L);
            return;
        }
        if (!node.isLeader()) {
            log.info("received an AppendLogEntriesResultMessage[{}], current node is not leader, ignore it.", message);
            return;
        }
        preHandle(message);
        Endpoint endpoint = context.getEndpoint(message.getSourceId());
        if (endpoint == null) {
            log.warn("node[{}] is not in cluster.", message.getSourceId());
            return;
        }
        boolean doReplication = false;
        if (message.isSuccess()) {
            if (endpoint.isMatchComplete()) {
                doReplication = endpoint.updateReplicationState(message.getLastLogIndex());
            } else {
                endpoint.updateMatchHelperState(true);
                doReplication = true;
                // 重置心跳时间，用于直接开始日志复制
                endpoint.setLastHeartBeat(0L);
            }

            int newCommitIndex = context.getNewCommitIndex();
            if (context.canAdvanceCommitIndex(newCommitIndex, currentTerm)) {
                context.getDataManager().advanceCommitIndex(newCommitIndex);
                context.advanceLastApplied(message.getLastLogIndex());
            }
        } else {
            doReplication = true;
            if (endpoint.isMatchComplete()) {
                endpoint.backoffNextIndex();
            } else {
                endpoint.updateMatchHelperState(false);
            }
            endpoint.setLastHeartBeat(0L);
        }
        if (doReplication) {
            context.doLogReplication(endpoint, currentTerm);
        }
    }

}
