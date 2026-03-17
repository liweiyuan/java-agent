package io.github.javaagent.api.log;

/**
 * 全局日志工厂，由 core 初始化
 */
public final class LoggerFactory {

    private static volatile Factory factory;

    public static AgentLogger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    public static AgentLogger getLogger(String name) {
        // 返回代理，每次调用时动态委托给当前 factory，避免静态字段缓存 NoopLogger
        return new DelegatingLogger(name);
    }

    public static void setFactory(Factory f) {
        factory = f;
    }

    public interface Factory {
        AgentLogger getLogger(String name);
    }

    private static AgentLogger resolve(String name) {
        if (factory == null) {
            return NoopLogger.INSTANCE;
        }
        return factory.getLogger(name);
    }

    private static final class DelegatingLogger implements AgentLogger {
        private final String name;
        DelegatingLogger(String name) { this.name = name; }
        public void debug(String msg)              { resolve(name).debug(msg); }
        public void info(String msg)               { resolve(name).info(msg); }
        public void warn(String msg)               { resolve(name).warn(msg); }
        public void error(String msg)              { resolve(name).error(msg); }
        public void error(String msg, Throwable t) { resolve(name).error(msg, t); }
    }

    private static final class NoopLogger implements AgentLogger {
        static final NoopLogger INSTANCE = new NoopLogger();
        public void debug(String msg) { System.out.println("[DEBUG] " + msg); }
        public void info(String msg)  { System.out.println("[INFO]  " + msg); }
        public void warn(String msg)  { System.out.println("[WARN]  " + msg); }
        public void error(String msg) { System.err.println("[ERROR] " + msg); }
        public void error(String msg, Throwable t) { error(msg); t.printStackTrace(); }
    }
}
