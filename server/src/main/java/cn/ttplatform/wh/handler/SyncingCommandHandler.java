package cn.ttplatform.wh.handler;

import cn.ttplatform.wh.GlobalContext;
import cn.ttplatform.wh.constant.DistributableType;
import cn.ttplatform.wh.constant.ErrorMessage;
import cn.ttplatform.wh.group.EndpointMetaData;
import cn.ttplatform.wh.support.AbstractDistributableHandler;
import cn.ttplatform.wh.support.Distributable;

/**
 * @author Wang Hao
 * @date 2021/5/3 10:27
 */
public class SyncingCommandHandler extends AbstractDistributableHandler {

    public SyncingCommandHandler(GlobalContext context) {
        super(context);
    }

    @Override
    public int getHandlerType() {
        return DistributableType.SYNCING;
    }

    @Override
    public void doHandleInClusterMode(Distributable distributable) {
        throw new UnsupportedOperationException(ErrorMessage.MESSAGE_TYPE_ERROR);
    }

    @Override
    public void doHandleInSingleMode(Distributable distributable) {
        SyncingCommand cmd = (SyncingCommand) distributable;
        EndpointMetaData followerMetaData = cmd.getFollowerMetaData();
        String clusterInfo = cmd.getLeaderMetaData().toString() + " " + followerMetaData.toString();
        context.setProperty("clusterInfo", clusterInfo);
        context.setProperty("connectorHost", followerMetaData.getHost());
        context.setProperty("connectorPort", followerMetaData.getConnectorPort());
        context.enterClusterMode();
        context.getNode().changeToFollower(cmd.getTerm(), cmd.getLeaderMetaData().getNodeId(), null, 0, 0, 0);
    }
}
