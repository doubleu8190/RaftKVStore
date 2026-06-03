package cn.ttplatform.wh.support;

import cn.ttplatform.wh.ClusterInfo;
import cn.ttplatform.wh.cmd.ClusterChangeResultCommand;
import cn.ttplatform.wh.cmd.Command;
import cn.ttplatform.wh.cmd.GetClusterInfoResultCommand;
import cn.ttplatform.wh.cmd.GetResultCommand;
import cn.ttplatform.wh.cmd.RedirectCommand;
import cn.ttplatform.wh.cmd.RequestFailedCommand;
import cn.ttplatform.wh.cmd.SetResultCommand;
import cn.ttplatform.wh.constant.DistributableType;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;

/**
 * Netty handler that processes server responses and dispatches them to pending
 * {@link ResponseFuture} instances.
 *
 * Handles:
 * - Normal responses: completes the matching future
 * - RedirectCommand: triggers leader reconnection via {@link RedirectHandler}
 * - Connection loss: fails all pending futures
 *
 * @author Wang Hao
 * @date 2021/5/26 21:25
 */
@Slf4j
@ChannelHandler.Sharable
public class ClientDuplexChannelHandler extends ChannelDuplexHandler {

    private final Map<String, ResponseFuture<?>> pendingRequests;
    private volatile RedirectHandler redirectHandler;

    public ClientDuplexChannelHandler() {
        this.pendingRequests = new ConcurrentHashMap<>();
    }

    public void setRedirectHandler(RedirectHandler redirectHandler) {
        this.redirectHandler = redirectHandler;
    }

    public Map<String, ResponseFuture<?>> getPendingRequests() {
        return pendingRequests;
    }

    /**
     * Register a pending future so it can be completed when the response arrives.
     */
    public void registerFuture(String commandId, ResponseFuture<?> future) {
        pendingRequests.put(commandId, future);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof Command) {
            Command command = (Command) msg;
            handleCommand(command);
        } else {
            log.warn("Received unexpected message type: {}", msg.getClass().getName());
        }
    }

    @SuppressWarnings("unchecked")
    private void handleCommand(Command command) {
        String commandId = command.getId();
        ResponseFuture<?> future = pendingRequests.remove(commandId);

        if (future == null) {
            log.debug("No pending future for command id: {} (type: {})", commandId, command.getType());
            return;
        }

        if (command instanceof RedirectCommand) {
            handleRedirect((RedirectCommand) command);
            return;
        }

        if (command instanceof RequestFailedCommand) {
            RequestFailedCommand failed = (RequestFailedCommand) command;
            future.completeExceptionally(
                new ClientException("Server rejected request: " + failed.getFailedMessage()));
            return;
        }

        try {
            if (future instanceof ResponseFuture) {
                completeFuture((ResponseFuture<Object>) future, command);
            } else {
                log.warn("Unknown future type for command id: {}", commandId);
            }
        } catch (Exception e) {
            log.error("Failed to handle response for command id: {}", commandId, e);
            future.completeExceptionally(
                new ClientException("Failed to process response", e));
        }
    }

    @SuppressWarnings("unchecked")
    private void completeFuture(ResponseFuture<Object> future, Command command) {
        switch (command.getType()) {
            case DistributableType.SET_COMMAND_RESULT:
                SetResultCommand setResult = (SetResultCommand) command;
                future.complete(setResult.isResult());
                break;
            case DistributableType.GET_COMMAND_RESULT:
                GetResultCommand getResult = (GetResultCommand) command;
                future.complete(getResult.getValue());
                break;
            case DistributableType.CLUSTER_CHANGE_RESULT_COMMAND:
                ClusterChangeResultCommand changeResult = (ClusterChangeResultCommand) command;
                future.complete(changeResult.isDone());
                break;
            case DistributableType.GET_CLUSTER_INFO_RESULT_COMMAND:
                GetClusterInfoResultCommand infoResult = (GetClusterInfoResultCommand) command;
                ClusterInfo info = ClusterInfo.builder()
                    .leader(infoResult.getLeader())
                    .phase(infoResult.getPhase())
                    .mode(infoResult.getMode())
                    .oldConfig(infoResult.getOldConfig())
                    .newConfig(infoResult.getNewConfig())
                    .size(infoResult.getSize())
                    .build();
                future.complete(info);
                break;
            default:
                log.warn("Unexpected response type {} for command id: {}", command.getType(),
                    command.getId());
                future.completeExceptionally(
                    new ClientException("Unexpected response type: " + command.getType()));
        }
    }

    private void handleRedirect(RedirectCommand redirect) {
        log.info("Received redirect to leader: {}, endpointMetaData: {}",
            redirect.getLeader(), redirect.getEndpointMetaData());

        // Fail all pending requests — they need to be re-sent to the new leader
        for (ResponseFuture<?> f : pendingRequests.values()) {
            f.completeExceptionally(
                new ClientException("Redirected to leader: " + redirect.getLeader()));
        }
        pendingRequests.clear();

        // Trigger reconnection to the leader
        if (redirectHandler != null) {
            redirectHandler.onRedirect(redirect.getLeader(), redirect.getEndpointMetaData());
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.warn("Channel inactive, failing {} pending requests.", pendingRequests.size());
        for (ResponseFuture<?> future : pendingRequests.values()) {
            future.completeExceptionally(
                new ClientException("Connection lost"));
        }
        pendingRequests.clear();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("Exception in client channel: {}", cause.getMessage(), cause);
        ctx.close();
    }

    /**
     * Callback interface for handling redirects.
     */
    @FunctionalInterface
    public interface RedirectHandler {
        void onRedirect(String leaderId, String endpointMetaData);
    }
}
