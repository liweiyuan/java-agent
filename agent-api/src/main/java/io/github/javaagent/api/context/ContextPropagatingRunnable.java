package io.github.javaagent.api.context;

/** 包装 Runnable，在子线程执行时恢复捕获的 Context。 */
public final class ContextPropagatingRunnable implements Runnable {

    private final Runnable delegate;
    private final Context capturedContext;

    public ContextPropagatingRunnable(Runnable delegate, Context capturedContext) {
        this.delegate = delegate;
        this.capturedContext = capturedContext;
    }

    @Override
    public void run() {
        try (Scope ignored = capturedContext.makeCurrent()) {
            delegate.run();
        }
    }
}
