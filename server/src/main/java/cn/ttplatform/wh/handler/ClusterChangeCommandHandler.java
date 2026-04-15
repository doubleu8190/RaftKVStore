package cn.ttplatform.wh.handler;

import cn.ttplatform.wh.GlobalContext;
import cn.ttplatform.wh.cmd.ClusterChangeCommand;
import cn.ttplatform.wh.cmd.RequestFailedCommand;
import cn.ttplatform.wh.constant.DistributableType;
import cn.ttplatform.wh.constant.ErrorMessage;
import cn.ttplatform.wh.group.EndpointMetaData;
import cn.ttplatform.wh.support.AbstractDistributableHandler;
import cn.ttplatform.wh.support.ChannelPool;
import cn.ttplatform.wh.support.Distributable;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;

/**
 * @author Wang Hao
 * @date 2021/4/23 23:26
 */
@Slf4j
public class ClusterChangeCommandHandler extends AbstractDistributableHandler {

    private final RequestFailedCommand requestFailedCommand = new RequestFailedCommand();
    private final ChannelPool channelPool;

    public ClusterChangeCommandHandler(GlobalContext context) {
        super(context);
        this.channelPool = context.getChannelPool();
    }

    @Override
    public void doHandleInSingleMode(Distributable distributable) {
        context.enterClusterMode();
        context.getNode().changeToLeader(context.getNode().getTerm());
        doHandleInClusterMode(distributable);
    }

    @Override
    public void doHandleInClusterMode(Distributable distributable) {
        ClusterChangeCommand cmd = (ClusterChangeCommand) distributable;
        log.info("receive an ClusterChangeCommand: {}", cmd);
        if (!context.setCurrentClusterChangeTask(cmd)) {
            requestFailedCommand.setId(cmd.getId());
            requestFailedCommand.setFailedMessage(ErrorMessage.CLUSTER_CHANGE_IN_PROGRESS);
            channelPool.reply(cmd.getId(), requestFailedCommand);
        } else {
            Set<String> newConfigStr = cmd.getNewConfig();
            Set<EndpointMetaData> newConfig = new HashSet<>((int) (newConfigStr.size() / 0.75f) + 1);
            newConfigStr.forEach(metaData -> newConfig.add(new EndpointMetaData(metaData)));
            // 如果本次更新没有新增节点，那么直接进入OLD_NEW阶段，否则进入SYNCING阶段
            if (context.updateNewConfig(newConfig)) {
                // If there is no added node, go directly to the OLD_NEW phase
                log.info("there is no added node, prepare to enter OLD_NEW phase");
                context.enterOldNewPhase();
            } else {
                context.enterSyncingPhase();
            }
        }
    }

    @Override
    public byte getHandlerType() {
        return DistributableType.CLUSTER_CHANGE_COMMAND;
    }
}
