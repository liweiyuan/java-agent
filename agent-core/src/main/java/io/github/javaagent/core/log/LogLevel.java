package io.github.javaagent.core.log;

/**
 * 日志级别
 */
public enum LogLevel {
    DEBUG, INFO, WARN, ERROR;

    public boolean isEnabled(LogLevel target) {
        return this.ordinal() <= target.ordinal();
    }
}
