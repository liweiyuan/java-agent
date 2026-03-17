package io.github.javaagent.core.trace;

import io.github.javaagent.api.trace.Span;
import io.github.javaagent.api.trace.SpanStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class DefaultSpan implements Span {

    private final String traceId;
    private final String spanId;
    private final String parentSpanId;
    private final String name;
    private final long startTimeNanos;
    private final Map<String, String> attributes = new HashMap<>();
    private SpanStatus status = SpanStatus.UNSET;
    private String statusDescription;
    private long endTimeNanos;

    DefaultSpan(String traceId, String parentSpanId, String name) {
        this.traceId = traceId;
        this.spanId = generateId();
        this.parentSpanId = parentSpanId;
        this.name = name;
        this.startTimeNanos = System.nanoTime();
    }

    @Override
    public String getTraceId() { return traceId; }

    @Override
    public String getSpanId() { return spanId; }

    @Override
    public String getParentSpanId() { return parentSpanId; }

    @Override
    public Span setAttribute(String key, String value) {
        attributes.put(key, value);
        return this;
    }

    @Override
    public Span setAttribute(String key, long value) {
        attributes.put(key, String.valueOf(value));
        return this;
    }

    @Override
    public Span setStatus(SpanStatus status, String description) {
        this.status = status;
        this.statusDescription = description;
        return this;
    }

    @Override
    public Span recordException(Throwable throwable) {
        setAttribute("exception.type", throwable.getClass().getName());
        setAttribute("exception.message", throwable.getMessage() != null ? throwable.getMessage() : "");
        setStatus(SpanStatus.ERROR, throwable.getMessage());
        return this;
    }

    @Override
    public void end() {
        this.endTimeNanos = System.nanoTime();
        SpanExporter.export(this);
    }

    public String getName() { return name; }
    public Map<String, String> getAttributes() { return attributes; }
    public SpanStatus getStatus() { return status; }
    public String getStatusDescription() { return statusDescription; }
    public long getDurationMillis() {
        return TimeUnit.NANOSECONDS.toMillis(endTimeNanos - startTimeNanos);
    }

    private static String generateId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
