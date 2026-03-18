package io.github.javaagent.api.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 描述一条字节码增强规则：匹配哪些类、哪些方法、用哪个 Advice。
 * 插件通过 {@link InstrumentationPlugin#transformations()} 返回规则列表。
 */
public final class Transformation {

    /** 目标类匹配规则（OR 关系） */
    public final List<TypeMatcher> typeMatchers;

    /** 目标方法匹配规则 */
    public final MethodMatcher methodMatcher;

    /** Advice 类的全限定名，由 agent-core 用 Class.forName 加载 */
    public final String adviceClassName;

    public Transformation(List<TypeMatcher> typeMatchers, MethodMatcher methodMatcher, String adviceClassName) {
        this.typeMatchers = typeMatchers;
        this.methodMatcher = methodMatcher;
        this.adviceClassName = adviceClassName;
    }

    public static Builder on(TypeMatcher... matchers) {
        return new Builder(Arrays.asList(matchers));
    }

    public static final class Builder {
        private final List<TypeMatcher> typeMatchers;

        Builder(List<TypeMatcher> typeMatchers) {
            this.typeMatchers = typeMatchers;
        }

        public Transformation withAdvice(MethodMatcher methodMatcher, Class<?> adviceClass) {
            return new Transformation(typeMatchers, methodMatcher, adviceClass.getName());
        }
    }
}
