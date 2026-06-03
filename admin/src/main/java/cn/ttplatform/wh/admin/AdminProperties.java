package cn.ttplatform.wh.admin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Admin module configuration loaded from admin.properties.
 *
 * @author Wang Hao
 * @date 2021/5/26 21:25
 */
@Data
@Slf4j
public class AdminProperties {

    private int adminPort;
    private String raftHost;
    private int raftPort;

    public AdminProperties() {
        Properties properties = new Properties();
        try (InputStream is = getClass().getClassLoader()
            .getResourceAsStream("admin.properties")) {
            if (is != null) {
                properties.load(is);
            } else {
                log.warn("admin.properties not found on classpath, using defaults.");
            }
        } catch (IOException e) {
            log.warn("Failed to load admin.properties, using defaults.", e);
        }
        configure(properties);
    }

    public AdminProperties(String configPath) {
        Properties properties = new Properties();
        File file = new File(configPath);
        try (FileInputStream fis = new FileInputStream(file)) {
            properties.load(fis);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load config from: " + configPath, e);
        }
        configure(properties);
    }

    private void configure(Properties properties) {
        String envAdminPort = System.getProperty("ADMIN_PORT");
        String envRaftHost = System.getProperty("RAFT_HOST");
        String envRaftPort = System.getProperty("RAFT_PORT");

        this.adminPort = Integer.parseInt(
            envAdminPort != null ? envAdminPort : properties.getProperty("admin.port", "9090"));
        this.raftHost = envRaftHost != null ? envRaftHost : properties.getProperty("raft.host", "127.0.0.1");
        this.raftPort = Integer.parseInt(
            envRaftPort != null ? envRaftPort : properties.getProperty("raft.port", "6666"));
    }
}
