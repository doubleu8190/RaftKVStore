package cn.ttplatform.wh.message.handler;

import cn.ttplatform.wh.message.RequestVoteMessage;
import cn.ttplatform.wh.message.RequestVoteResultMessage;
import cn.ttplatform.wh.constant.DistributableType;
import cn.ttplatform.wh.GlobalContext;
import cn.ttplatform.wh.Node;
import cn.ttplatform.wh.role.Follower;
import cn.ttplatform.wh.role.Role;
import cn.ttplatform.wh.support.AbstractDistributableHandler;
import cn.ttplatform.wh.support.Distributable;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Wang Hao
 * @date 2021/2/17 1:46
 */
@Slf4j
public class RequestVoteMessageHandler extends AbstractDistributableHandler {

    public RequestVoteMessageHandler(GlobalContext context) {
        super(context);
    }

    @Override
    public int getHandlerType() {
        return DistributableType.REQUEST_VOTE;
    }

    @Override
    public void doHandleInClusterMode(Distributable distributable) {
        RequestVoteMessage message = (RequestVoteMessage) distributable;
        RequestVoteResultMessage resultMessage = process(message);
        if (resultMessage != null) {
            context.sendMessage(resultMessage, message.getSourceId());
        }
    }

    private RequestVoteResultMessage process(RequestVoteMessage message) {
        Node node = context.getNode();
        Role role = node.getRole();
        int term = message.getTerm();
        int currentTerm = role.getTerm();
        int lastLogIndex = message.getLastLogIndex();
        int lastLogTerm = message.getLastLogTerm();
        String candidateId = message.getSourceId();
        RequestVoteResultMessage requestVoteResultMessage = RequestVoteResultMessage.builder()
                .isVoted(Boolean.FALSE).term(currentTerm)
                .build();
        if (term < currentTerm) {
            log.debug("the term[{}] < currentTerm[{}], reject this request vote message.", term, currentTerm);
            return requestVoteResultMessage;
        }
        if (term > currentTerm) {
            log.debug("the term[{}] > currentTerm[{}], become follower", term, currentTerm);
            node.changeToFollower(term, null, null, 0, 0, 0L);
            currentTerm = term;
            role = node.getRole();
        }
        requestVoteResultMessage.setTerm(term);
        if (node.isFollower() && System.currentTimeMillis() - ((Follower) role).getLastHeartBeat() < context.getProperties()
                .getMinElectionTimeout()) {
            log.debug("current leader is alive, reject this request vote message.");
            return requestVoteResultMessage;
        }
        if (!node.isFollower()) {
            return requestVoteResultMessage;
        }
        boolean upToDate = !context.getDataManager().isNewerThan(lastLogIndex, lastLogTerm);
        String voteTo = ((Follower) role).getVoteTo();
        boolean canVote = voteTo == null || voteTo.isEmpty() || candidateId.equals(voteTo);
        boolean voted = upToDate && canVote;
        log.debug("the term[{}] >= currentTerm[{}], and the vote result is {}.", term, currentTerm, voted);
        requestVoteResultMessage.setVoted(voted);
        if (voted) {
            node.changeToFollower(currentTerm, null, candidateId, 0, 0, 0L);
        }
        return requestVoteResultMessage;
    }

}
