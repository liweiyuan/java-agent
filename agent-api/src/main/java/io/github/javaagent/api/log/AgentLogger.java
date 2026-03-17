package io.github.javaagent.api.log;

/**
 * Agent 日志接口
 */
public interface AgentLogger {

    void debug(String msg);
    void info(String msg);
    void warn(String msg);
    void error(String msg);
    void error(String msg, Throwable t);
}
