package io.github.javaagent.core.log;

/**
 * 日志配置，对应 agent.yaml 中的 logging 节点
 */
public class LogConfig {

    // 日志级别
    public LogLevel level = LogLevel.INFO;

    // 控制台输出
    public boolean consoleEnabled = true;

    // 文件输出
    public boolean fileEnabled = false;
    public String filePath = "logs/agent.log";

    // 单个文件最大大小（MB）
    public int fileSizeMb = 10;

    // 最多保留文件数
    public int fileMaxBackups = 5;
}
