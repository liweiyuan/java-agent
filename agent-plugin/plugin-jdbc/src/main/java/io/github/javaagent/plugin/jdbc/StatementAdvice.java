package io.github.javaagent.plugin.jdbc;

import io.github.javaagent.api.context.Scope;
import io.github.javaagent.api.trace.GlobalTracer;
import io.github.javaagent.api.trace.Span;
import io.github.javaagent.api.trace.SpanStatus;
import io.github.javaagent.api.trace.Tracer;
import net.bytebuddy.asm.Advice;

public class StatementAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static Object[] onEnter(@Advice.Argument(0) String sql) {
        Tracer tracer = GlobalTracer.get();
        if (tracer == null) return null;
        Tracer.SpanWithScope ss = tracer.spanBuilder("DB Query").start();
        ss.getSpan().setAttribute("db.statement", sql);
        ss.getSpan().setAttribute("db.system", "sql");
        return new Object[]{ss.getSpan(), ss.getScope()};
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void onExit(@Advice.Enter Object[] state, @Advice.Thrown Throwable thrown) {
        if (state == null) return;
        Span span = (Span) state[0];
        Scope scope = (Scope) state[1];
        try {
            if (thrown != null) span.recordException(thrown);
            else span.setStatus(SpanStatus.OK, null);
            span.end();
        } finally {
            scope.close();
        }
    }
}
