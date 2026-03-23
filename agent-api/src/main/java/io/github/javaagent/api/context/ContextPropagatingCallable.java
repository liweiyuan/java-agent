package io.github.javaagent.api.context;

import java.util.concurrent.Callable;

/** 包装 Callable，在子线程执行时恢复捕获的 Context。 */
public final class ContextPropagatingCallable<V> implements Callable<V> {

    private final Callable<V> delegate;
    private final Context capturedContext;

    public ContextPropagatingCallable(Callable<V> delegate, Context capturedContext) {
        this.delegate = delegate;
        this.capturedContext = capturedContext;
    }

    @Override
    public V call() throws Exception {
        try (Scope ignored = capturedContext.makeCurrent()) {
            return delegate.call();
        }
    }
}
