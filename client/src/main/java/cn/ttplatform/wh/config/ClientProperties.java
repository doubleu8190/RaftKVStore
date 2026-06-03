package cn.ttplatform.wh.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Client configuration loaded from client.properties.
 *
 * @author Wang Hao
 * @date 2021/3/15 15:25
 */
@Data
@Slf4j
public class ClientProperties {

    private String master;
    private String host;
    private int port;

    public ClientProperties() {
        Properties properties = new Properties();
        try (InputStream is = getClass().getClassLoader()
            .getResourceAsStream("client.properties")) {
            if (is != null) {
                properties.load(is);
            } else {
                log.warn("client.properties not found on classpath, using defaults.");
            }
        } catch (IOException e) {
            log.warn("Failed to load client.properties, using defaults.", e);
        }
        configure(properties);
    }

    public ClientProperties(String configPath) {
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
        String envHost = System.getProperty("ENV_HOST");
        String envPort = System.getProperty("ENV_PORT");
        String envMaster = System.getProperty("ENV_MASTER");

        this.master = envMaster != null ? envMaster : properties.getProperty("master", "A");
        this.host = envHost != null ? envHost : properties.getProperty("host", "127.0.0.1");
        this.port = Integer.parseInt(
            envPort != null ? envPort : properties.getProperty("port", "6666"));
    }
}
