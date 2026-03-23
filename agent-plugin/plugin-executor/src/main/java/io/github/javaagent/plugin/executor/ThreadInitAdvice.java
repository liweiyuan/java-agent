package io.github.javaagent.plugin.executor;

import io.github.javaagent.api.context.Context;
import io.github.javaagent.api.context.ExecutorFilter;
import io.github.javaagent.api.context.ThreadContextRegistry;
import net.bytebuddy.asm.Advice;

/** 增强 Thread 构造方法，在主线程创建 Thread 时捕获当前 Context。 */
public class ThreadInitAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(@Advice.This Thread thread) {
        if (!ExecutorFilter.matches(thread.getClass().getName())) {
            return;
        }
        Context ctx = Context.current();
        ThreadContextRegistry.capture(thread, ctx);
    }
}
