package io.github.javaagent.api.trace;

import io.github.javaagent.api.context.ContextKey;

/**
 * 代表一次操作的追踪单元，参考 OpenTelemetry Span
 */
public interface Span {

    ContextKey<Span> CONTEXT_KEY = ContextKey.named("span");

    String getTraceId();

    String getSpanId();

    String getParentSpanId();

    Span setAttribute(String key, String value);

    Span setAttribute(String key, long value);

    Span setStatus(SpanStatus status, String description);

    Span recordException(Throwable throwable);

    void end();

    static Span current() {
        return io.github.javaagent.api.context.Context.current().get(CONTEXT_KEY);
    }
}
