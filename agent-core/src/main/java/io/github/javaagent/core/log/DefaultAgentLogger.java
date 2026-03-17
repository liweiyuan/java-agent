package io.github.javaagent.core.log;

import io.github.javaagent.api.log.AgentLogger;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DefaultAgentLogger implements AgentLogger {

    private static final ThreadLocal<SimpleDateFormat> DATE_FORMAT =
            new ThreadLocal<SimpleDateFormat>() {
                @Override protected SimpleDateFormat initialValue() {
                    return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                }
            };

    private final String name;
    private final LogConfig config;
    private final RollingFileAppender fileAppender;

    DefaultAgentLogger(String name, LogConfig config, RollingFileAppender fileAppender) {
        this.name = abbreviate(name);
        this.config = config;
        this.fileAppender = fileAppender;
    }

    @Override public void debug(String msg) { log(LogLevel.DEBUG, msg, null); }
    @Override public void info(String msg)  { log(LogLevel.INFO,  msg, null); }
    @Override public void warn(String msg)  { log(LogLevel.WARN,  msg, null); }
    @Override public void error(String msg) { log(LogLevel.ERROR, msg, null); }
    @Override public void error(String msg, Throwable t) { log(LogLevel.ERROR, msg, t); }

    private void log(LogLevel level, String msg, Throwable t) {
        if (!config.level.isEnabled(level)) return;

        String line = format(level, msg, t);
        if (config.consoleEnabled) {
            if (level == LogLevel.ERROR || level == LogLevel.WARN) {
                System.err.println(line);
            } else {
                System.out.println(line);
            }
        }
        if (config.fileEnabled && fileAppender != null) {
            fileAppender.append(line);
        }
    }

    private String format(LogLevel level, String msg, Throwable t) {
        StringBuilder sb = new StringBuilder();
        sb.append(DATE_FORMAT.get().format(new Date()))
          .append(" [").append(level.name()).append("] ")
          .append("[").append(name).append("] ")
          .append(msg);
        if (t != null) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            sb.append(System.lineSeparator()).append(sw);
        }
        return sb.toString();
    }

    // 缩短包名：io.github.javaagent.core.AgentBootstrap -> i.g.j.c.AgentBootstrap
    private static String abbreviate(String name) {
        int lastDot = name.lastIndexOf('.');
        if (lastDot < 0) return name;
        String[] parts = name.substring(0, lastDot).split("\\.");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) sb.append(p.charAt(0)).append('.');
        sb.append(name.substring(lastDot + 1));
        return sb.toString();
    }
}
