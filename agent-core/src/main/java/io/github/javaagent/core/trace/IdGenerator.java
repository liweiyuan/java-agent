package io.github.javaagent.core.trace;

/**
 * ID 生成器接口，用于生成 traceId 和 spanId。
 * 可通过 {@link IdGenerator#set(IdGenerator)} 替换实现。
 */
public interface IdGenerator {

    String generateTraceId();

    String generateSpanId();

    /** 获取当前全局实例 */
    static IdGenerator get() {
        return Holder.INSTANCE;
    }

    /** 替换全局实现，需在 agent 启动阶段调用 */
    static void set(IdGenerator generator) {
        Holder.INSTANCE = generator;
    }

    class Holder {
        static volatile IdGenerator INSTANCE = new ThreadLocalIdGenerator();
    }
}
