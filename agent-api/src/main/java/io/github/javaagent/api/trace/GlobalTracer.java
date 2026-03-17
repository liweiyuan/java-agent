package io.github.javaagent.api.trace;

/**
 * 全局 Tracer 持有者，由 agent-core 在启动时注入实现。
 * 此类会被注入到 bootstrap classloader，Advice 可直接引用。
 */
public final class GlobalTracer {

    private static volatile Tracer instance;

    public static void set(Tracer tracer) {
        instance = tracer;
    }

    public static Tracer get() {
        return instance;
    }
}
