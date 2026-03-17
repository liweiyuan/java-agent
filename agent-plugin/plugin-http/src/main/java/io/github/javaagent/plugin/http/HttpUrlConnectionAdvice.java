package io.github.javaagent.plugin.http;

import io.github.javaagent.api.context.Scope;
import io.github.javaagent.api.trace.GlobalTracer;
import io.github.javaagent.api.trace.Span;
import io.github.javaagent.api.trace.SpanStatus;
import io.github.javaagent.api.trace.Tracer;
import net.bytebuddy.asm.Advice;

import java.net.HttpURLConnection;

public class HttpUrlConnectionAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static Object[] onEnter(@Advice.This HttpURLConnection connection) {
        Tracer tracer = GlobalTracer.get();
        if (tracer == null) return null;
        Tracer.SpanWithScope ss = tracer.spanBuilder("HTTP " + connection.getRequestMethod()).start();
        ss.getSpan().setAttribute("http.method", connection.getRequestMethod());
        ss.getSpan().setAttribute("http.url", connection.getURL().toString());
        return new Object[]{ss.getSpan(), ss.getScope()};
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void onExit(
            @Advice.Enter Object[] state,
            @Advice.Return int responseCode,
            @Advice.Thrown Throwable thrown) {
        if (state == null) return;
        Span span = (Span) state[0];
        Scope scope = (Scope) state[1];
        try {
            if (thrown != null) {
                span.recordException(thrown);
            } else {
                span.setAttribute("http.status_code", responseCode);
                span.setStatus(responseCode >= 400 ? SpanStatus.ERROR : SpanStatus.OK, null);
            }
            span.end();
        } finally {
            scope.close();
        }
    }
}
