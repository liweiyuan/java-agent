package io.github.javaagent.api.context;

/**
 * Context 存储策略，默认基于 ThreadLocal
 */
public interface ContextStorage {

    Context current();

    Scope attach(Context context);

    static ContextStorage get() {
        return ThreadLocalContextStorage.INSTANCE;
    }
}
