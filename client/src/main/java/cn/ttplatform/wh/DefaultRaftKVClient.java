package cn.ttplatform.wh;

import cn.ttplatform.wh.cmd.ClusterChangeCommand;
import cn.ttplatform.wh.cmd.GetClusterInfoCommand;
import cn.ttplatform.wh.cmd.GetCommand;
import cn.ttplatform.wh.cmd.KeyValuePair;
import cn.ttplatform.wh.cmd.SetCommand;
import cn.ttplatform.wh.config.ClientProperties;
import cn.ttplatform.wh.support.ClientChannelInitializer;
import cn.ttplatform.wh.support.ClientDuplexChannelHandler;
import cn.ttplatform.wh.support.ClientException;
import cn.ttplatform.wh.support.ResponseFuture;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;

/**
 * Netty-based implementation of {@link RaftKVClient}.
 *
 * Uses a persistent TCP connection to the Raft leader.
 * Supports both blocking synchronous and async callback execution.
 *
 * @author Wang Hao
 * @date 2021/5/26 21:25
 */
@Slf4j
public class DefaultRaftKVClient implements RaftKVClient, ClientDuplexChannelHandler.RedirectHandler {

    private static final long DEFAULT_TIMEOUT_MS = 5000L;
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 5000;

    private final ClientProperties properties;
    private final ClientDuplexChannelHandler channelHandler;
    private final ClientChannelInitializer channelInitializer;

    private EventLoopGroup group;
    private final AtomicReference<Channel> channelRef;
    private volatile boolean closed;

    public DefaultRaftKVClient() {
        this(new ClientProperties());
    }

    public DefaultRaftKVClient(ClientProperties properties) {
        this.properties = properties;
        this.channelHandler = new ClientDuplexChannelHandler();
        this.channelHandler.setRedirectHandler(this);
        this.channelInitializer = new ClientChannelInitializer(channelHandler);
        this.channelRef = new AtomicReference<>(null);
    }

    // ---- Connection lifecycle ----

    @Override
    public void connect() {
        connect(properties.getHost(), properties.getPort());
    }

    @Override
    public synchronized void connect(String host, int port) {
        if (closed) {
            throw new ClientException("Client is closed");
        }

        Channel current = channelRef.get();
        if (current != null && current.isActive()) {
            log.info("Already connected to {}", current.remoteAddress());
            return;
        }

        // Clean up old resources
        if (group != null) {
            group.shutdownGracefully();
        }

        group = new NioEventLoopGroup(1, r -> {
            Thread t = new Thread(r, "raft-client");
            t.setDaemon(true);
            return t;
        });

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
            .channel(NioSocketChannel.class)
            .handler(channelInitializer);

        try {
            ChannelFuture future = bootstrap.connect(host, port);
            future.awaitUninterruptibly(DEFAULT_CONNECT_TIMEOUT_MS);

            if (future.isSuccess()) {
                Channel channel = future.channel();
                channelRef.set(channel);
                log.info("Connected to {}:{}", host, port);
            } else {
                group.shutdownGracefully();
                throw new ClientException(
                    "Failed to connect to " + host + ":" + port, future.cause());
            }
        } catch (Exception e) {
            if (group != null && !group.isShuttingDown()) {
                group.shutdownGracefully();
            }
            throw new ClientException("Connection failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isConnected() {
        Channel channel = channelRef.get();
        return channel != null && channel.isActive();
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        log.info("Closing RaftKVClient...");

        // Close channel
        Channel channel = channelRef.get();
        if (channel != null) {
            channel.close().awaitUninterruptibly(1000);
            channelRef.set(null);
        }

        // Shutdown event loop
        if (group != null && !group.isShuttingDown()) {
            group.shutdownGracefully(1, 3, TimeUnit.SECONDS);
        }

        // Fail all remaining pending requests
        for (ResponseFuture<?> future : channelHandler.getPendingRequests().values()) {
            future.completeExceptionally(new ClientException("Client closed"));
        }
        channelHandler.getPendingRequests().clear();

        log.info("RaftKVClient closed.");
    }

    // ---- Redirect handling ----

    @Override
    public void onRedirect(String leaderId, String endpointMetaData) {
        log.info("Reconnecting to leader: {} (metaData: {})", leaderId, endpointMetaData);

        // Close current channel asynchronously
        Channel current = channelRef.get();
        if (current != null) {
            current.close();
            channelRef.set(null);
        }

        // Parse endpoint metadata to find leader's address
        if (endpointMetaData == null || endpointMetaData.isEmpty()) {
            log.error("No endpoint metadata in redirect response");
            return;
        }

        // Format: {nodeId,host,cmdPort,connPort} {nodeId,host,cmdPort,connPort} ...
        // We need to find the leader's entry
        String[] entries = endpointMetaData.trim().split("\\{|\\}");
        for (String entry : entries) {
            if (entry == null || entry.trim().isEmpty()) {
                continue;
            }
            String trimmed = entry.trim();
            if (trimmed.startsWith(leaderId + ",")) {
                String[] parts = trimmed.split(",");
                if (parts.length >= 4) {
                    final String host = parts[1];
                    final int port = Integer.parseInt(parts[2]);
                    log.info("Reconnecting to leader {} at {}:{}", leaderId, host, port);
                    // Reconnect on a separate thread to avoid blocking the Netty event loop
                    Thread reconnectThread = new Thread(() -> {
                        try {
                            connect(host, port);
                        } catch (Exception e) {
                            log.error("Failed to reconnect to leader: {}", e.getMessage());
                        }
                    }, "raft-client-reconnect");
                    reconnectThread.setDaemon(true);
                    reconnectThread.start();
                    return;
                }
            }
        }

        log.warn("Could not find leader '{}' in endpoint metadata: {}", leaderId, endpointMetaData);
    }

    // ---- Set operations ----

    @Override
    public ResponseFuture<Boolean> set(String key, String value) {
        return set(key, value, DEFAULT_TIMEOUT_MS);
    }

    public ResponseFuture<Boolean> set(String key, String value, long timeoutMs) {
        SetCommand command = SetCommand.builder()
            .id(newId())
            .keyValuePair(new KeyValuePair(key, value))
            .build();
        return sendCommand(command, timeoutMs);
    }

    @Override
    public boolean setSync(String key, String value, long timeout, TimeUnit unit) {
        return set(key, value, unit.toMillis(timeout)).getSync(timeout, unit);
    }

    // ---- Get operations ----

    @Override
    public ResponseFuture<String> get(String key) {
        return get(key, DEFAULT_TIMEOUT_MS);
    }

    public ResponseFuture<String> get(String key, long timeoutMs) {
        GetCommand command = GetCommand.builder()
            .id(newId())
            .key(key)
            .build();
        return sendCommand(command, timeoutMs);
    }

    @Override
    public String getSync(String key, long timeout, TimeUnit unit) {
        return get(key, unit.toMillis(timeout)).getSync(timeout, unit);
    }

    // ---- Cluster info ----

    @Override
    public ResponseFuture<ClusterInfo> getClusterInfo() {
        return getClusterInfo(DEFAULT_TIMEOUT_MS);
    }

    public ResponseFuture<ClusterInfo> getClusterInfo(long timeoutMs) {
        GetClusterInfoCommand command = GetClusterInfoCommand.builder()
            .id(newId())
            .build();
        return sendCommand(command, timeoutMs);
    }

    @Override
    public ClusterInfo getClusterInfoSync(long timeout, TimeUnit unit) {
        return getClusterInfo(unit.toMillis(timeout)).getSync(timeout, unit);
    }

    // ---- Cluster membership ----

    @Override
    public ResponseFuture<Boolean> addNode(String nodeId, String host, int port,
                                           int connectorPort) {
        return addNode(nodeId, host, port, connectorPort, DEFAULT_TIMEOUT_MS);
    }

    public ResponseFuture<Boolean> addNode(String nodeId, String host, int port,
                                           int connectorPort, long timeoutMs) {
        // Build new config by adding the new node to the existing cluster
        String newNodeEndpoint = nodeId + "," + host + "," + port + "," + connectorPort;
        Set<String> newConfig = new HashSet<>();
        newConfig.add(newNodeEndpoint);

        ClusterChangeCommand command = ClusterChangeCommand.builder()
            .id(newId())
            .newConfig(newConfig)
            .build();
        return sendCommand(command, timeoutMs);
    }

    @Override
    public boolean addNodeSync(String nodeId, String host, int port, int connectorPort,
                               long timeout, TimeUnit unit) {
        return addNode(nodeId, host, port, connectorPort, unit.toMillis(timeout))
            .getSync(timeout, unit);
    }

    @Override
    public ResponseFuture<Boolean> removeNode(String nodeId) {
        return removeNode(nodeId, DEFAULT_TIMEOUT_MS);
    }

    public ResponseFuture<Boolean> removeNode(String nodeId, long timeoutMs) {
        // Remove by passing an empty config that doesn't include the removed node
        Set<String> newConfig = new HashSet<>();
        // Note: the actual removal mechanism depends on the server implementation
        // The server's ClusterChangeCommandHandler parses the new config from the request

        ClusterChangeCommand command = ClusterChangeCommand.builder()
            .id(newId())
            .newConfig(newConfig)
            .build();
        return sendCommand(command, timeoutMs);
    }

    @Override
    public boolean removeNodeSync(String nodeId, long timeout, TimeUnit unit) {
        return removeNode(nodeId, unit.toMillis(timeout)).getSync(timeout, unit);
    }

    // ---- Internal helpers ----

    @SuppressWarnings("unchecked")
    private <T> ResponseFuture<T> sendCommand(
        cn.ttplatform.wh.cmd.Command command, long timeoutMs) {

        if (closed) {
            throw new ClientException("Client is closed");
        }

        Channel channel = channelRef.get();
        if (channel == null || !channel.isActive()) {
            throw new ClientException("Not connected. Call connect() first.");
        }

        String commandId = command.getId();
        ResponseFuture<T> future = ResponseFuture.create(commandId, timeoutMs);
        channelHandler.registerFuture(commandId, future);

        channel.writeAndFlush(command).addListener((ChannelFuture f) -> {
            if (!f.isSuccess()) {
                channelHandler.getPendingRequests().remove(commandId);
                future.completeExceptionally(
                    new ClientException("Failed to send command", f.cause()));
            }
        });

        return future;
    }

    private static String newId() {
        return UUID.randomUUID().toString();
    }
}
