package io.github.javaagent.plugin.executor;

import io.github.javaagent.api.context.Context;
import io.github.javaagent.api.context.Scope;
import io.github.javaagent.api.context.ThreadContextRegistry;
import net.bytebuddy.asm.Advice;

/** 增强 Thread.run()，恢复构造时捕获的 Context。 */
public class ThreadRunAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static Scope onEnter(@Advice.This Thread thread) {
        Context ctx = ThreadContextRegistry.get(thread);
        if (ctx == null) return null;
        return ctx.makeCurrent();
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void onExit(@Advice.Enter Scope scope, @Advice.This Thread thread) {
        if (scope != null) {
            scope.close();
        }
        ThreadContextRegistry.remove(thread);
    }
}
