package io.github.javaagent.core.log;

import io.github.javaagent.api.log.AgentLogger;
import io.github.javaagent.api.log.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 日志系统初始化入口
 */
public class LoggingSystem {

    private static RollingFileAppender fileAppender;
    private static LogConfig config;
    private static final ConcurrentMap<String, AgentLogger> cache = new ConcurrentHashMap<>();

    public static void init(LogConfig logConfig) {
        config = logConfig;
        if (logConfig.fileEnabled) {
            try {
                fileAppender = new RollingFileAppender(logConfig.filePath, logConfig.fileSizeMb, logConfig.fileMaxBackups);
            } catch (IOException e) {
                System.err.println("[JavaAgent] Failed to init file appender: " + e.getMessage());
            }
        }
        LoggerFactory.setFactory(new LoggerFactory.Factory() {
            @Override
            public AgentLogger getLogger(String name) {
                AgentLogger logger = cache.get(name);
                if (logger == null) {
                    logger = new DefaultAgentLogger(name, config, fileAppender);
                    cache.putIfAbsent(name, logger);
                }
                return cache.get(name);
            }
        });
    }
}
