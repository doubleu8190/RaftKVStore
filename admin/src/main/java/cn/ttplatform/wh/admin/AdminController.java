package cn.ttplatform.wh.admin;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.staticfiles.Location;
import lombok.extern.slf4j.Slf4j;

/**
 * Registers REST API routes and configures static file serving for the admin UI.
 *
 * @author Wang Hao
 * @date 2021/5/26 21:25
 */
@Slf4j
public class AdminController {

    private final RaftAdminService adminService;

    public AdminController(RaftAdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Create and configure the Javalin application.
     */
    public Javalin createApp() {
        Javalin app = Javalin.create(config -> {
            // Serve static files from classpath:/public
            config.staticFiles.add(staticFileConfig -> {
                staticFileConfig.hostedPath = "/";
                staticFileConfig.directory = "/public";
                staticFileConfig.location = Location.CLASSPATH;
            });

            // Enable CORS for development convenience
            config.plugins.enableCors(cors -> {
                cors.add(it -> it.anyHost());
            });
        });

        // ---- API Routes ----

        app.get("/api/status", this::handleStatus);
        app.post("/api/set", this::handleSet);
        app.post("/api/get", this::handleGet);
        app.get("/api/cluster-info", this::handleClusterInfo);
        app.post("/api/add-node", this::handleAddNode);
        app.post("/api/remove-node", this::handleRemoveNode);

        // Global exception handler
        app.exception(Exception.class, (e, ctx) -> {
            log.error("Unhandled error on {} {}: {}", ctx.method(), ctx.path(), e.getMessage(), e);
            ctx.status(500).json(RaftAdminService.ServiceResult.error("Internal server error: " + e.getMessage()));
        });

        return app;
    }

    // ---- Handlers ----

    private void handleStatus(Context ctx) {
        boolean connected = adminService.isConnected();
        ctx.json(RaftAdminService.ServiceResult.ok(connected));
    }

    private void handleSet(Context ctx) {
        SetRequest req = ctx.bodyAsClass(SetRequest.class);
        if (req.key() == null || req.key().isEmpty()) {
            ctx.json(RaftAdminService.ServiceResult.error("key is required"));
            return;
        }
        RaftAdminService.ServiceResult result = adminService.set(req.key(), req.value());
        ctx.json(result);
    }

    private void handleGet(Context ctx) {
        GetRequest req = ctx.bodyAsClass(GetRequest.class);
        if (req.key() == null || req.key().isEmpty()) {
            ctx.json(RaftAdminService.ServiceResult.error("key is required"));
            return;
        }
        RaftAdminService.ServiceResult result = adminService.get(req.key());
        ctx.json(result);
    }

    private void handleClusterInfo(Context ctx) {
        RaftAdminService.ServiceResult result = adminService.getClusterInfo();
        ctx.json(result);
    }

    private void handleAddNode(Context ctx) {
        AddNodeRequest req = ctx.bodyAsClass(AddNodeRequest.class);
        if (req.nodeId() == null || req.nodeId().isEmpty()) {
            ctx.json(RaftAdminService.ServiceResult.error("nodeId is required"));
            return;
        }
        if (req.host() == null || req.host().isEmpty()) {
            ctx.json(RaftAdminService.ServiceResult.error("host is required"));
            return;
        }
        RaftAdminService.ServiceResult result = adminService.addNode(
            req.nodeId(), req.host(), req.port(), req.connectorPort());
        ctx.json(result);
    }

    private void handleRemoveNode(Context ctx) {
        RemoveNodeRequest req = ctx.bodyAsClass(RemoveNodeRequest.class);
        if (req.nodeId() == null || req.nodeId().isEmpty()) {
            ctx.json(RaftAdminService.ServiceResult.error("nodeId is required"));
            return;
        }
        RaftAdminService.ServiceResult result = adminService.removeNode(req.nodeId());
        ctx.json(result);
    }

    // ---- Request DTOs ----

    public record SetRequest(String key, String value) {}
    public record GetRequest(String key) {}
    public record AddNodeRequest(String nodeId, String host, int port, int connectorPort) {}
    public record RemoveNodeRequest(String nodeId) {}
}
