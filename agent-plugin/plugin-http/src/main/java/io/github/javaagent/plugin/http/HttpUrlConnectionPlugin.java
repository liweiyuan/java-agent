package io.github.javaagent.plugin.http;

import io.github.javaagent.api.plugin.InstrumentationPlugin;
import io.github.javaagent.api.plugin.MethodMatcher;
import io.github.javaagent.api.plugin.Transformation;
import io.github.javaagent.api.plugin.TypeMatcher;

import java.util.Collections;
import java.util.List;

public class HttpUrlConnectionPlugin implements InstrumentationPlugin {

    @Override
    public String name() {
        return "http-urlconnection";
    }

    @Override
    public List<Transformation> transformations() {
        return Collections.singletonList(
                Transformation.on(
                        TypeMatcher.named("java.net.HttpURLConnection"),
                        TypeMatcher.subtypeOf(java.net.HttpURLConnection.class)
                ).withAdvice(
                        MethodMatcher.named("getResponseCode"),
                        HttpUrlConnectionAdvice.class
                )
        );
    }
}
