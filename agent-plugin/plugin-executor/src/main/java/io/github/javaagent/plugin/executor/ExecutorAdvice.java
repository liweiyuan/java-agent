package io.github.javaagent.plugin.executor;

import io.github.javaagent.api.context.Context;
import io.github.javaagent.api.context.ContextPropagatingRunnable;
import io.github.javaagent.api.context.ExecutorFilter;
import net.bytebuddy.asm.Advice;

/** 增强 ThreadPoolExecutor.execute()，包装 Runnable 以传播 Context。 */
public class ExecutorAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(@Advice.Argument(value = 0, readOnly = false) Runnable runnable) {
        if (runnable == null || runnable instanceof ContextPropagatingRunnable) {
            return;
        }
        if (!ExecutorFilter.matches(runnable.getClass().getName())) {
            return;
        }
        Context ctx = Context.current();
        if (ctx != Context.root()) {
            runnable = new ContextPropagatingRunnable(runnable, ctx);
        }
    }
}
