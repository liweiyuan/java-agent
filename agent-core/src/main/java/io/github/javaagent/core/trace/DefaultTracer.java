package io.github.javaagent.core.trace;

import io.github.javaagent.api.context.Context;
import io.github.javaagent.api.context.Scope;
import io.github.javaagent.api.trace.Span;
import io.github.javaagent.api.trace.Tracer;

import java.util.UUID;

public class DefaultTracer implements Tracer {

    public static final DefaultTracer INSTANCE = new DefaultTracer();

    @Override
    public SpanBuilder spanBuilder(String spanName) {
        return new DefaultSpanBuilder(spanName);
    }

    private static class DefaultSpanBuilder implements SpanBuilder {

        private final String spanName;
        private Context parentContext;

        DefaultSpanBuilder(String spanName) {
            this.spanName = spanName;
            this.parentContext = Context.current();
        }

        @Override
        public SpanBuilder setParent(Context context) {
            this.parentContext = context;
            return this;
        }

        @Override
        public SpanBuilder setAttribute(String key, String value) {
            // stored after span creation
            return this;
        }

        @Override
        public SpanWithScope start() {
            Span parentSpan = parentContext.get(Span.CONTEXT_KEY);

            String traceId = parentSpan != null ? parentSpan.getTraceId()
                    : UUID.randomUUID().toString().replace("-", "");
            String parentSpanId = parentSpan != null ? parentSpan.getSpanId() : null;

            DefaultSpan span = new DefaultSpan(traceId, parentSpanId, spanName);
            Context newContext = parentContext.with(Span.CONTEXT_KEY, span);
            Scope scope = newContext.makeCurrent();

            return new SpanWithScope() {
                @Override public Span getSpan() { return span; }
                @Override public Scope getScope() { return scope; }
            };
        }
    }
}
