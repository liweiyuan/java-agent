package io.github.javaagent.api.context;

/**
 * 请求上下文，贯穿整个调用链，参考 OpenTelemetry Context
 */
public interface Context {

    /**
     * 获取上下文中的值
     */
    <V> V get(ContextKey<V> key);

    /**
     * 返回一个新的 Context，包含指定 key-value
     */
    <V> Context with(ContextKey<V> key, V value);

    /**
     * 将当前 Context 设置为当前线程的活跃 Context，返回 Scope 用于关闭
     */
    Scope makeCurrent();

    /**
     * 获取当前线程的活跃 Context
     */
    static Context current() {
        return ContextStorage.get().current();
    }

    /**
     * 获取根 Context
     */
    static Context root() {
        return DefaultContext.ROOT;
    }
}
