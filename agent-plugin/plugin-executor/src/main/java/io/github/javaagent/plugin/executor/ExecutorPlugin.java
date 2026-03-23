package io.github.javaagent.plugin.executor;

import io.github.javaagent.api.context.ExecutorFilter;
import io.github.javaagent.api.plugin.InstrumentationPlugin;
import io.github.javaagent.api.plugin.MethodMatcher;
import io.github.javaagent.api.plugin.Transformation;
import io.github.javaagent.api.plugin.TypeMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

public class ExecutorPlugin implements InstrumentationPlugin {

    @Override
    public String name() {
        return "executor";
    }

    @Override
    public void init(Map<String, String> config) {
        List<String> packages = splitConfig(config.get("plugin.executor.packages"));
        List<String> patterns = splitConfig(config.get("plugin.executor.patterns"));
        ExecutorFilter.init(packages, patterns);
    }

    private static List<String> splitConfig(String value) {
        List<String> result = new ArrayList<String>();
        if (value == null || value.trim().isEmpty()) return result;
        for (String item : value.split(",")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) result.add(trimmed);
        }
        return result;
    }

    @Override
    public List<Transformation> transformations() {
        return Arrays.asList(
                Transformation.on(TypeMatcher.subtypeOf(ThreadPoolExecutor.class))
                        .withAdvice(MethodMatcher.named("execute"), ExecutorAdvice.class),
                Transformation.on(TypeMatcher.named("java.lang.Thread"))
                        .withAdvice(MethodMatcher.isConstructor(), ThreadInitAdvice.class),
                Transformation.on(TypeMatcher.named("java.lang.Thread"))
                        .withAdvice(MethodMatcher.named("run"), ThreadRunAdvice.class)
        );
    }
}
