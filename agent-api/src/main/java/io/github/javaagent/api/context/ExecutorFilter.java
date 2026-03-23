package io.github.javaagent.api.context;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 运行时过滤器，判断 Runnable/Callable 实例是否需要传播 Context。
 * 由 agent-core 在插件初始化时注入配置。
 */
public final class ExecutorFilter {

    private static volatile List<String> packagePrefixes = null;
    private static volatile List<Pattern> patterns = null;

    private ExecutorFilter() {}

    public static void init(List<String> packages, List<String> regexPatterns) {
        packagePrefixes = packages;
        List<Pattern> compiled = new java.util.ArrayList<Pattern>();
        for (String p : regexPatterns) {
            compiled.add(Pattern.compile(p));
        }
        patterns = compiled;
    }

    /**
     * 判断给定类名是否需要传播 Context。
     * 未配置时（两个列表均为空）返回 true，即增强所有。
     */
    public static boolean matches(String className) {
        List<String> pkgs = packagePrefixes;
        List<Pattern> pts = patterns;
        if ((pkgs == null || pkgs.isEmpty()) && (pts == null || pts.isEmpty())) {
            return true;
        }
        if (pkgs != null) {
            for (String prefix : pkgs) {
                if (className.startsWith(prefix)) return true;
            }
        }
        if (pts != null) {
            for (Pattern p : pts) {
                if (p.matcher(className).matches()) return true;
            }
        }
        return false;
    }
}
