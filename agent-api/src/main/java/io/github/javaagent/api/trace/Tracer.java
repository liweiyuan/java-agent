package io.github.javaagent.api.trace;

import io.github.javaagent.api.context.Context;
import io.github.javaagent.api.context.Scope;

/**
 * 创建和管理 Span
 */
public interface Tracer {

    SpanBuilder spanBuilder(String spanName);

    interface SpanBuilder {

        SpanBuilder setParent(Context context);

        SpanBuilder setAttribute(String key, String value);

        /**
         * 开始 Span 并将其设置到当前 Context，返回 Scope
         */
        SpanWithScope start();
    }

    interface SpanWithScope {
        Span getSpan();
        Scope getScope();
    }
}
