package cn.ttplatform.wh.admin;

import cn.ttplatform.wh.DefaultRaftKVClient;
import cn.ttplatform.wh.config.ClientProperties;
import io.javalin.Javalin;
import lombok.extern.slf4j.Slf4j;

/**
 * Entry point for the RaftKVStore admin web UI.
 *
 * Starts an HTTP server and connects to the Raft cluster via TCP.
 * The admin UI is available at http://localhost:{adminPort}.
 *
 * @author Wang Hao
 * @date 2021/5/26 21:25
 */
@Slf4j
public class AdminApplication {

    public static void main(String[] args) {
        // Load configuration
        AdminProperties adminProps = new AdminProperties();

        // Build Raft client properties (connect to the Raft cluster)
        ClientProperties clientProps = new ClientProperties();
        clientProps.setHost(adminProps.getRaftHost());
        clientProps.setPort(adminProps.getRaftPort());

        // Create and connect the Raft client
        DefaultRaftKVClient client = new DefaultRaftKVClient(clientProps);
        try {
            client.connect();
            log.info("Connected to Raft cluster at {}:{}",
                adminProps.getRaftHost(), adminProps.getRaftPort());
        } catch (Exception e) {
            log.warn("Failed to connect to Raft cluster at {}:{}: {}. "
                + "Admin UI will start but operations will fail until the cluster is available.",
                adminProps.getRaftHost(), adminProps.getRaftPort(), e.getMessage());
        }

        // Build service and controller
        RaftAdminService adminService = new RaftAdminService(client);
        AdminController controller = new AdminController(adminService);

        // Create and start the HTTP server
        Javalin app = controller.createApp();
        app.start(adminProps.getAdminPort());

        log.info("========================================");
        log.info("  RaftKVStore Admin UI started at:");
        log.info("  http://localhost:{}", adminProps.getAdminPort());
        log.info("========================================");

        // Graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down admin server...");
            app.stop();
            client.close();
            log.info("Admin server stopped.");
        }, "admin-shutdown"));
    }
}
