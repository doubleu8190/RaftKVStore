package cn.ttplatform.wh.group;

import io.protostuff.Exclude;
import java.net.InetSocketAddress;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author Wang Hao
 * @date 2021/4/21 13:30
 */
@Data
@AllArgsConstructor
public class EndpointMetaData {

    private static final int SEGMENT_NUM = 4;
    private static final int NODE_ID_INDEX = 0;
    private static final int HOST_INDEX = 1;
    private static final int COMMAND_PORT_INDEX = 2;
    private static final int CONNECTOR_PORT_INDEX = 3;
    private final String nodeId;
    private final int commandPort;
    private final int connectorPort;
    private final String host;
    @Exclude
    private volatile InetSocketAddress connectorAddress;
    @Exclude
    private volatile InetSocketAddress commandAddress;

    public EndpointMetaData(String metaData) {
        String[] segments = metaData.split(",");
        if (segments.length != SEGMENT_NUM) {
            throw new IllegalArgumentException("illegal node info [" + metaData + "]");
        }
        nodeId = segments[NODE_ID_INDEX];
        host = segments[HOST_INDEX];
        try {
            commandPort = Integer.parseInt(segments[COMMAND_PORT_INDEX]);
            connectorPort = Integer.parseInt(segments[CONNECTOR_PORT_INDEX]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("illegal port in node info [" + metaData + "]");
        }
    }

    public InetSocketAddress getCommandAddress() {
        if (commandAddress==null){
            synchronized (EndpointMetaData.class){
                if (commandAddress==null){
                    commandAddress = new InetSocketAddress(host, commandPort);
                }
            }
        }
        return commandAddress;
    }

    public InetSocketAddress getConnectorAddress() {
        if (connectorAddress==null){
            synchronized (EndpointMetaData.class){
                if (connectorAddress==null){
                    connectorAddress = new InetSocketAddress(host, commandPort);
                }
            }
        }
        return connectorAddress;
    }

    @Override
    public String toString() {
        return nodeId + "," + host + "," + commandPort + "," + connectorPort;
    }
}
