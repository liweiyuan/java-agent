package io.github.javaagent.api.context;

/**
 * Context 中的 key，类型安全
 */
public final class ContextKey<T> {

    private final String name;

    private ContextKey(String name) {
        this.name = name;
    }

    public static <T> ContextKey<T> named(String name) {
        return new ContextKey<>(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
