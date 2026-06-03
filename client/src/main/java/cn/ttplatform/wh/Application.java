package cn.ttplatform.wh;

import cn.ttplatform.wh.config.ClientProperties;
import cn.ttplatform.wh.support.ClientException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;

/**
 * CLI entry point for the RaftKVStore client.
 *
 * Usage:
 * <pre>
 *   java -jar client.jar set &lt;key&gt; &lt;value&gt; [--host HOST] [--port PORT]
 *   java -jar client.jar get &lt;key&gt; [--host HOST] [--port PORT]
 *   java -jar client.jar cluster-info [--host HOST] [--port PORT]
 *   java -jar client.jar add-node &lt;id&gt; &lt;host&gt; &lt;port&gt; &lt;connectorPort&gt;
 *   java -jar client.jar remove-node &lt;id&gt;
 *   java -jar client.jar demo [--host HOST] [--port PORT]    # run async demo
 * </pre>
 *
 * @author Wang Hao
 * @date 2021/5/27 10:00
 */
@Slf4j
public class Application {

    private static final long TIMEOUT_SECONDS = 5;

    public static void main(String[] args) {
        if (args.length == 0) {
            printUsage();
            System.exit(1);
        }

        String command = args[0].toLowerCase();
        String host = extractOption(args, "--host", "127.0.0.1");
        int port = Integer.parseInt(extractOption(args, "--port", "6666"));

        ClientProperties properties = new ClientProperties();
        properties.setHost(host);
        properties.setPort(port);

        DefaultRaftKVClient client = new DefaultRaftKVClient(properties);

        try {
            client.connect();

            switch (command) {
                case "set":
                    handleSet(client, args);
                    break;
                case "get":
                    handleGet(client, args);
                    break;
                case "cluster-info":
                    handleClusterInfo(client);
                    break;
                case "add-node":
                    handleAddNode(client, args);
                    break;
                case "remove-node":
                    handleRemoveNode(client, args);
                    break;
                case "demo":
                    runDemo(client);
                    break;
                default:
                    System.err.println("Unknown command: " + command);
                    printUsage();
                    System.exit(1);
            }
        } catch (ClientException e) {
            System.err.println("Error: " + e.getMessage());
            System.exit(1);
        } finally {
            client.close();
        }
    }

    // ---- Command handlers ----

    private static void handleSet(DefaultRaftKVClient client, String[] args) {
        if (args.length < 3) {
            System.err.println("Usage: set <key> <value> [--host HOST] [--port PORT]");
            System.exit(1);
        }
        String key = args[1];
        String value = args[2];

        System.out.println("Setting " + key + " = " + value + " ...");
        boolean success = client.setSync(key, value, TIMEOUT_SECONDS, TimeUnit.SECONDS);
        System.out.println("Result: " + (success ? "OK" : "FAILED"));
    }

    private static void handleGet(DefaultRaftKVClient client, String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: get <key> [--host HOST] [--port PORT]");
            System.exit(1);
        }
        String key = args[1];

        System.out.println("Getting " + key + " ...");
        String value = client.getSync(key, TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (value == null) {
            System.out.println("(nil)");
        } else {
            System.out.println("\"" + value + "\"");
        }
    }

    private static void handleClusterInfo(DefaultRaftKVClient client) {
        System.out.println("Fetching cluster info ...");
        ClusterInfo info = client.getClusterInfoSync(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        System.out.println("Leader:      " + info.getLeader());
        System.out.println("Mode:        " + info.getMode());
        System.out.println("Phase:       " + info.getPhase());
        System.out.println("Old Config:  " + info.getOldConfig());
        System.out.println("New Config:  " + info.getNewConfig());
        System.out.println("Key Count:   " + info.getSize());
    }

    private static void handleAddNode(DefaultRaftKVClient client, String[] args) {
        if (args.length < 5) {
            System.err.println(
                "Usage: add-node <id> <host> <port> <connectorPort> [--host HOST] [--port PORT]");
            System.exit(1);
        }
        String nodeId = args[1];
        String host = args[2];
        int port = Integer.parseInt(args[3]);
        int connectorPort = Integer.parseInt(args[4]);

        System.out.println("Adding node " + nodeId + " ...");
        boolean success = client.addNodeSync(nodeId, host, port, connectorPort,
            TIMEOUT_SECONDS, TimeUnit.SECONDS);
        System.out.println("Result: " + (success ? "OK" : "FAILED"));
    }

    private static void handleRemoveNode(DefaultRaftKVClient client, String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: remove-node <id> [--host HOST] [--port PORT]");
            System.exit(1);
        }
        String nodeId = args[1];

        System.out.println("Removing node " + nodeId + " ...");
        boolean success = client.removeNodeSync(nodeId, TIMEOUT_SECONDS, TimeUnit.SECONDS);
        System.out.println("Result: " + (success ? "OK" : "FAILED"));
    }

    // ---- Async demo ----

    private static void runDemo(DefaultRaftKVClient client) {
        System.out.println("=== RaftKVStore Client Demo (Async) ===");
        System.out.println();

        // Async set with callback
        System.out.println("1. Sending async SET foo=bar ...");
        client.set("foo", "bar")
            .thenAccept(success -> {
                if (success) {
                    System.out.println("   [async] SET foo=bar -> OK");
                } else {
                    System.out.println("   [async] SET foo=bar -> FAILED");
                }
            });

        // Small delay to allow the set to complete before get
        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Async get with callback
        System.out.println("2. Sending async GET foo ...");
        client.get("foo")
            .thenAccept(value -> {
                if (value != null) {
                    System.out.println("   [async] GET foo -> \"" + value + "\"");
                } else {
                    System.out.println("   [async] GET foo -> (nil)");
                }
            });

        // Small delay
        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Async set another key
        System.out.println("3. Sending async SET hello=world ...");
        client.set("hello", "world")
            .thenAccept(success -> {
                if (success) {
                    System.out.println("   [async] SET hello=world -> OK");
                } else {
                    System.out.println("   [async] SET hello=world -> FAILED");
                }
            });

        // Small delay
        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        // Sync cluster info (for comparison)
        System.out.println("4. Getting cluster info (sync) ...");
        ClusterInfo info = client.getClusterInfoSync(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        System.out.println("   Leader:    " + info.getLeader());
        System.out.println("   Mode:      " + info.getMode());
        System.out.println("   Key Count: " + info.getSize());

        // Async get cluster info with callback
        System.out.println("5. Getting cluster info (async) ...");
        client.getClusterInfo()
            .thenAccept(ci -> System.out.println(
                "   [async] Leader: " + ci.getLeader() + ", Phase: " + ci.getPhase()));

        // Wait for the last async callback
        try { Thread.sleep(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        System.out.println();
        System.out.println("=== Demo Complete ===");
    }

    // ---- Utilities ----

    private static void printUsage() {
        System.out.println("RaftKVStore Client");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java -jar client.jar set <key> <value> [--host HOST] [--port PORT]");
        System.out.println("  java -jar client.jar get <key> [--host HOST] [--port PORT]");
        System.out.println("  java -jar client.jar cluster-info [--host HOST] [--port PORT]");
        System.out.println("  java -jar client.jar add-node <id> <host> <port> <connectorPort>");
        System.out.println("  java -jar client.jar remove-node <id>");
        System.out.println("  java -jar client.jar demo [--host HOST] [--port PORT]");
        System.out.println();
        System.out.println("Environment variables:");
        System.out.println("  ENV_HOST      - Server host (default: 127.0.0.1)");
        System.out.println("  ENV_PORT      - Server port (default: 6666)");
        System.out.println("  ENV_MASTER    - Expected master node ID (default: A)");
    }

    private static String extractOption(String[] args, String option, String defaultValue) {
        for (int i = 0; i < args.length - 1; i++) {
            if (option.equals(args[i])) {
                return args[i + 1];
            }
        }
        return defaultValue;
    }
}
