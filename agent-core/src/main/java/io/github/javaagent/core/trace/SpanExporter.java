package io.github.javaagent.core.trace;

import io.github.javaagent.api.log.AgentLogger;
import io.github.javaagent.api.log.LoggerFactory;

public class SpanExporter {

    private static final AgentLogger log = LoggerFactory.getLogger(SpanExporter.class);

    public static void export(DefaultSpan span) {
        log.info(String.format(
                "[Span] name=%s traceId=%s spanId=%s parentSpanId=%s status=%s duration=%dms attrs=%s",
                span.getName(), span.getTraceId(), span.getSpanId(), span.getParentSpanId(),
                span.getStatus(), span.getDurationMillis(), span.getAttributes()
        ));
    }
}
