package io.github.javaagent.api.context;

/**
 * 基于 ThreadLocal 的 Context 存储
 */
final class ThreadLocalContextStorage implements ContextStorage {

    static final ThreadLocalContextStorage INSTANCE = new ThreadLocalContextStorage();

    private static final ThreadLocal<Context> CONTEXT = new ThreadLocal<Context>() {
        @Override
        protected Context initialValue() {
            return DefaultContext.ROOT;
        }
    };

    @Override
    public Context current() {
        return CONTEXT.get();
    }

    @Override
    public Scope attach(final Context context) {
        final Context previous = CONTEXT.get();
        CONTEXT.set(context);
        return new Scope() {
            @Override
            public void close() {
                CONTEXT.set(previous);
            }
        };
    }
}
