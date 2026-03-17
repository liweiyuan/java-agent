package io.github.javaagent.core;

import java.net.URL;
import java.net.URLClassLoader;

/**
 * Agent 专用 ClassLoader。
 * 对 agent-api 包强制委托给 bootstrap classloader，确保全局只有一份，
 * 避免 AppClassLoader 重复加载导致静态字段（LoggerFactory.factory、GlobalTracer）隔离。
 */
public class AgentClassLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public AgentClassLoader(URL[] urls) {
        // parent 设为 null，即 bootstrap classloader
        super(urls, null);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        synchronized (getClassLoadingLock(name)) {
            Class<?> c = findLoadedClass(name);
            if (c != null) return c;

            // agent-api 包 以及 AgentBootstrap/AgentClassLoader 本身：强制走 bootstrap，保证全局唯一
            if (name.startsWith("io.github.javaagent.api.")
                    || name.equals("io.github.javaagent.core.AgentBootstrap")
                    || name.equals("io.github.javaagent.core.AgentClassLoader")) {
                return Class.forName(name, false, null);
            }

            // 其余 agent 内部类（core/plugin）：从自身 jar 加载，与应用隔离
            if (name.startsWith("io.github.javaagent.")) {
                try {
                    c = findClass(name);
                    if (resolve) resolveClass(c);
                    return c;
                } catch (ClassNotFoundException ignored) {}
            }

            // JDK 类走 bootstrap
            try {
                return Class.forName(name, false, null);
            } catch (ClassNotFoundException ignored) {}

            return getSystemClassLoader().loadClass(name);
        }
    }
}
