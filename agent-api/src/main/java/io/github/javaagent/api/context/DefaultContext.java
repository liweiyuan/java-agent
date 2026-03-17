package io.github.javaagent.api.context;

import java.util.HashMap;
import java.util.Map;

/**
 * 不可变的 Context 默认实现
 */
final class DefaultContext implements Context {

    static final Context ROOT = new DefaultContext(new HashMap<ContextKey<?>, Object>());

    private final Map<ContextKey<?>, Object> data;

    DefaultContext(Map<ContextKey<?>, Object> data) {
        this.data = data;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V get(ContextKey<V> key) {
        return (V) data.get(key);
    }

    @Override
    public <V> Context with(ContextKey<V> key, V value) {
        Map<ContextKey<?>, Object> newData = new HashMap<>(data);
        newData.put(key, value);
        return new DefaultContext(newData);
    }

    @Override
    public Scope makeCurrent() {
        return ContextStorage.get().attach(this);
    }
}
