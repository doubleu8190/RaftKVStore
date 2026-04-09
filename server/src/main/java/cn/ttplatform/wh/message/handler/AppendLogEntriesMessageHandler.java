package cn.ttplatform.wh.message.handler;

import cn.ttplatform.wh.GlobalContext;
import cn.ttplatform.wh.Node;
import cn.ttplatform.wh.constant.DistributableType;
import cn.ttplatform.wh.data.DataManager;
import cn.ttplatform.wh.exception.IncorrectLogIndexNumberException;
import cn.ttplatform.wh.message.AppendLogEntriesMessage;
import cn.ttplatform.wh.message.AppendLogEntriesResultMessage;
import cn.ttplatform.wh.role.Follower;
import cn.ttplatform.wh.role.Role;
import cn.ttplatform.wh.role.RoleType;
import cn.ttplatform.wh.support.AbstractDistributableHandler;
import cn.ttplatform.wh.support.Distributable;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Wang Hao
 * @date 2021/2/17 0:22
 */
@Slf4j
public class AppendLogEntriesMessageHandler extends AbstractDistributableHandler {

    public AppendLogEntriesMessageHandler(GlobalContext context) {
        super(context);
    }

    @Override
    public int getHandlerType() {
        return DistributableType.APPEND_LOG_ENTRIES;
    }

    @Override
    public void doHandleInClusterMode(Distributable distributable) {
        AppendLogEntriesMessage message = (AppendLogEntriesMessage) distributable;
        try {
            context.sendMessage(process(message), message.getSourceId());
            context.enterStablePhase();
        } catch (IncorrectLogIndexNumberException e) {
            log.warn(e.getMessage());
        }
    }

    private AppendLogEntriesResultMessage process(AppendLogEntriesMessage message) {
        int term = message.getTerm();
        Role role = context.getNode().getRole();
        int currentTerm = role.getTerm();
        if (term < currentTerm) {
            log.info("receive append entries message with term {}, current term is {}, ignore", term, currentTerm);
            return new AppendLogEntriesResultMessage(currentTerm, message.getLastIndex(), false);
        }
        if (role.getType() == RoleType.LEADER && term == currentTerm) {
            log.warn("receive append entries message from another leader {}, step down to follower", message.getSourceId());
        }
        Node node = context.getNode();
        String newLeaderId = message.getSourceId();
        // 需要记录下当前的voteTo，避免因为网络分区的情况下给其他leader投票，出现重复投票的情况
        String voteTo = null;
        if (node.getTerm() == term) {
            if (node.isFollower()) {
                voteTo = ((Follower) node.getRole()).getVoteTo();
            } else if (node.isCandidate()) {
                voteTo = node.getSelfId();
            }
        }
        node.changeToFollower(term, newLeaderId, voteTo, 0, 0, System.currentTimeMillis());

        DataManager dataManager = context.getDataManager();
        int preLogIndex = message.getPreLogIndex();
        boolean checkIndexAndTermIfMatched = dataManager.checkIndexAndTermIfMatched(preLogIndex, message.getPreLogTerm());
        if (checkIndexAndTermIfMatched && !message.isMatchComplete()) {
            return new AppendLogEntriesResultMessage(term, message.getLastIndex(), true);
        }
        if (checkIndexAndTermIfMatched) {
            log.debug("checkIndexAndTerm Matched");
            dataManager.pendingLogs(preLogIndex, message.getLogs());
            int indexOfLastNewEntry = message.getLastIndex();
            int newCommitIndex = Math.min(indexOfLastNewEntry, message.getLeaderCommitIndex());
            if (context.canAdvanceCommitIndex(newCommitIndex, term)){
                context.getDataManager().advanceCommitIndex(newCommitIndex);
                context.advanceLastApplied(message.getLeaderCommitIndex());
            }
            return new AppendLogEntriesResultMessage(term, message.getLastIndex(), true);
        }
        return new AppendLogEntriesResultMessage(term, message.getLastIndex(), false);
    }

}
