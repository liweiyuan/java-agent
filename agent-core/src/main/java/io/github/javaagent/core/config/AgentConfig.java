package io.github.javaagent.core.config;

import io.github.javaagent.core.log.LogConfig;
import io.github.javaagent.core.log.LogLevel;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 极简 agent.yaml 解析器（不依赖第三方库）
 * 只支持两层缩进的 key: value 格式
 */
public class AgentConfig {

    public final LogConfig logging = new LogConfig();

    /** 原始扁平 map，供插件读取自定义配置 */
    public Map<String, String> rawConfig = new HashMap<>();

    public static AgentConfig load(File yamlFile) {
        AgentConfig config = new AgentConfig();
        if (!yamlFile.exists()) return config;

        Map<String, String> flat = parseFlat(yamlFile);
        config.rawConfig = flat;

        String level = flat.get("logging.level");
        if (level != null) {
            try { config.logging.level = LogLevel.valueOf(level.toUpperCase()); } catch (Exception ignored) {}
        }
        config.logging.consoleEnabled = parseBool(flat, "logging.console.enabled", config.logging.consoleEnabled);
        config.logging.fileEnabled    = parseBool(flat, "logging.file.enabled", config.logging.fileEnabled);

        String filePath = flat.get("logging.file.path");
        if (filePath != null) config.logging.filePath = filePath;

        config.logging.fileSizeMb    = parseInt(flat, "logging.file.size-mb", config.logging.fileSizeMb);
        config.logging.fileMaxBackups = parseInt(flat, "logging.file.max-backups", config.logging.fileMaxBackups);

        return config;
    }

    /**
     * 将 yaml 展开为 "parent.child: value" 的扁平 map，列表项存为 "key[0]", "key[1]"
     */
    private static Map<String, String> parseFlat(File file) {
        Map<String, String> result = new HashMap<>();
        String[] parents = new String[10];
        int[] indents = new int[10];
        int depth = 0;
        Map<String, Integer> listCounters = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.trim().startsWith("#")) continue;

                int indent = 0;
                while (indent < line.length() && line.charAt(indent) == ' ') indent++;
                String trimmed = line.trim();

                // 处理列表项 "- value"
                if (trimmed.startsWith("- ")) {
                    String value = trimmed.substring(2).trim();
                    // 构建父 key
                    StringBuilder parentKey = new StringBuilder();
                    for (int i = 0; i < depth; i++) {
                        if (i > 0) parentKey.append('.');
                        parentKey.append(parents[i]);
                    }
                    String pk = parentKey.toString();
                    int idx = listCounters.containsKey(pk) ? listCounters.get(pk) : 0;
                    result.put(pk + "[" + idx + "]", value);
                    listCounters.put(pk, idx + 1);
                    continue;
                }

                int colonIdx = trimmed.indexOf(':');
                if (colonIdx < 0) continue;

                String key = trimmed.substring(0, colonIdx).trim();
                String value = trimmed.substring(colonIdx + 1).trim();

                // 确定当前深度
                while (depth > 0 && indent <= indents[depth - 1]) depth--;
                parents[depth] = key;
                indents[depth] = indent;

                if (!value.isEmpty()) {
                    StringBuilder fullKey = new StringBuilder();
                    for (int i = 0; i <= depth; i++) {
                        if (i > 0) fullKey.append('.');
                        fullKey.append(parents[i]);
                    }
                    result.put(fullKey.toString(), value);
                } else {
                    depth++;
                }
            }
        } catch (IOException ignored) {}
        return result;
    }

    private static boolean parseBool(Map<String, String> map, String key, boolean def) {
        String v = map.get(key);
        return v != null ? Boolean.parseBoolean(v) : def;
    }

    private static int parseInt(Map<String, String> map, String key, int def) {
        String v = map.get(key);
        if (v == null) return def;
        try { return Integer.parseInt(v); } catch (Exception e) { return def; }
    }
}
