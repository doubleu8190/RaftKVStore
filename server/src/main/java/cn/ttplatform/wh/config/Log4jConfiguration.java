package cn.ttplatform.wh.config;

import java.util.Properties;
import org.apache.log4j.LogManager;
import org.apache.log4j.PropertyConfigurator;

/**
 * @author Wang Hao
 * @date 2021/11/7 21:47
 */
public class Log4jConfiguration {

    private static final String PATTERN_LAYOUT = "org.apache.log4j.PatternLayout";
    private static final String APPENDER = "cn.ttplatform.wh.log4j.DailyRollingFileAppenderWrapper";
    private static final String FORMAT = "[%d{yyyy-MM-dd HH:mm:ss,SSS}] [%t] [%-5p] [%l]: %m%n";
    private static final String DEFAULT_DIR =
        System.getProperty("user.home") + System.getProperty("path.separator") + "raftkvstore";

    private final Properties properties;

    public Log4jConfiguration(Properties properties) {
        this.properties = properties;
        mergeProperties();
    }

    public void mergeProperties(){
        properties.putIfAbsent("log4j.rootLogger", "debug, stdout, D, I, W, E");
        properties.putIfAbsent("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
        properties.putIfAbsent("log4j.appender.stdout.layout", PATTERN_LAYOUT);
        properties.putIfAbsent("log4j.appender.stdout.layout.ConversionPattern", FORMAT);

        properties.putIfAbsent("log4j.appender.D", APPENDER);
        properties.putIfAbsent("log4j.appender.D.File", DEFAULT_DIR + "debug.log");
        properties.putIfAbsent("log4j.appender.D.Append", true);
        properties.putIfAbsent("log4j.appender.D.Threshold", "DEBUG");
        properties.putIfAbsent("log4j.appender.D.ImmediateFlush", false);
        properties.putIfAbsent("log4j.appender.D.BufferedIO", true);
        properties.putIfAbsent("log4j.appender.D.BufferSize", 8192);
        properties.putIfAbsent("log4j.appender.D.layout", PATTERN_LAYOUT);
        properties.putIfAbsent("log4j.appender.D.layout.ConversionPattern", FORMAT);

        properties.putIfAbsent("log4j.appender.I", APPENDER);
        properties.putIfAbsent("log4j.appender.I.File", DEFAULT_DIR + "info.log");
        properties.putIfAbsent("log4j.appender.I.Append", true);
        properties.putIfAbsent("log4j.appender.I.Threshold", "INFO");
        properties.putIfAbsent("log4j.appender.I.ImmediateFlush", false);
        properties.putIfAbsent("log4j.appender.I.BufferedIO", true);
        properties.putIfAbsent("log4j.appender.I.BufferSize", 8192);
        properties.putIfAbsent("log4j.appender.I.layout", PATTERN_LAYOUT);
        properties.putIfAbsent("log4j.appender.I.layout.ConversionPattern", FORMAT);

        properties.putIfAbsent("log4j.appender.W", APPENDER);
        properties.putIfAbsent("log4j.appender.W.File", DEFAULT_DIR + "warn.log");
        properties.putIfAbsent("log4j.appender.W.Append", true);
        properties.putIfAbsent("log4j.appender.W.Threshold", "WARN");
        properties.putIfAbsent("log4j.appender.W.ImmediateFlush", false);
        properties.putIfAbsent("log4j.appender.W.BufferedIO", true);
        properties.putIfAbsent("log4j.appender.W.BufferSize", 8192);
        properties.putIfAbsent("log4j.appender.W.layout", PATTERN_LAYOUT);
        properties.putIfAbsent("log4j.appender.W.layout.ConversionPattern", FORMAT);

        properties.putIfAbsent("log4j.appender.E", APPENDER);
        properties.putIfAbsent("log4j.appender.E.File", DEFAULT_DIR + "error.log");
        properties.putIfAbsent("log4j.appender.E.Append", true);
        properties.putIfAbsent("log4j.appender.E.Threshold", "ERROR");
        properties.putIfAbsent("log4j.appender.E.ImmediateFlush", false);
        properties.putIfAbsent("log4j.appender.E.BufferedIO", true);
        properties.putIfAbsent("log4j.appender.E.BufferSize", 8192);
        properties.putIfAbsent("log4j.appender.E.layout", PATTERN_LAYOUT);
        properties.putIfAbsent("log4j.appender.E.layout.ConversionPattern", PATTERN_LAYOUT);
    }

    public void configure() {
        PropertyConfigurator configurator = new PropertyConfigurator();
        configurator.doConfigure(properties, LogManager.getLoggerRepository());
    }
}
