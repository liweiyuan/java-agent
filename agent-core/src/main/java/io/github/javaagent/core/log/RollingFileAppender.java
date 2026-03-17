package io.github.javaagent.core.log;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 滚动文件写入器，按文件大小滚动，保留最多 maxBackups 个历史文件
 */
class RollingFileAppender implements Closeable {

    private final File logFile;
    private final long maxBytes;
    private final int maxBackups;
    private BufferedWriter writer;
    private long currentSize;

    RollingFileAppender(String filePath, int sizeMb, int maxBackups) throws IOException {
        this.logFile = new File(filePath);
        this.maxBytes = (long) sizeMb * 1024 * 1024;
        this.maxBackups = maxBackups;

        if (logFile.getParentFile() != null) logFile.getParentFile().mkdirs();
        this.currentSize = logFile.exists() ? logFile.length() : 0;
        this.writer = new BufferedWriter(new FileWriter(logFile, true));
    }

    synchronized void append(String line) {
        try {
            if (currentSize >= maxBytes) roll();
            writer.write(line);
            writer.newLine();
            writer.flush();
            currentSize += line.length() + 1;
        } catch (IOException ignored) {}
    }

    private void roll() throws IOException {
        writer.close();
        // 删除最老的备份
        File oldest = new File(logFile.getPath() + "." + maxBackups);
        if (oldest.exists()) oldest.delete();
        // 依次重命名
        for (int i = maxBackups - 1; i >= 1; i--) {
            File f = new File(logFile.getPath() + "." + i);
            if (f.exists()) f.renameTo(new File(logFile.getPath() + "." + (i + 1)));
        }
        logFile.renameTo(new File(logFile.getPath() + ".1"));
        currentSize = 0;
        writer = new BufferedWriter(new FileWriter(logFile, false));
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
