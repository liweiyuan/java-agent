package io.github.javaagent.core;

import io.github.javaagent.api.plugin.InstrumentationPlugin;
import io.github.javaagent.api.plugin.MethodMatcher;
import io.github.javaagent.api.plugin.Transformation;
import io.github.javaagent.api.plugin.TypeMatcher;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * 将 {@link InstrumentationPlugin} 描述的规则翻译为 Byte Buddy 增强配置。
 */
public final class PluginInstaller {

    private PluginInstaller() {}

    public static AgentBuilder install(AgentBuilder agentBuilder, InstrumentationPlugin plugin, ClassLoader pluginClassLoader) {
        ClassFileLocator locator = ClassFileLocator.ForClassLoader.of(pluginClassLoader);
        for (Transformation t : plugin.transformations()) {
            Class<?> adviceClass = loadAdvice(t.adviceClassName, pluginClassLoader);
            agentBuilder = agentBuilder
                    .type(buildTypeMatcher(t.typeMatchers))
                    .transform((builder, type, cl, module, pd) ->
                            builder.visit(Advice.to(adviceClass, locator)
                                    .on(buildMethodMatcher(t.methodMatcher))));
        }
        return agentBuilder;
    }

    private static ElementMatcher<net.bytebuddy.description.type.TypeDescription> buildTypeMatcher(java.util.List<TypeMatcher> matchers) {
        ElementMatcher.Junction<net.bytebuddy.description.type.TypeDescription> result = ElementMatchers.none();
        for (TypeMatcher m : matchers) {
            result = result.or(toElementMatcher(m));
        }
        return result;
    }

    private static ElementMatcher.Junction<net.bytebuddy.description.type.TypeDescription> toElementMatcher(TypeMatcher m) {
        switch (m.strategy) {
            case NAMED:      return ElementMatchers.named(m.className);
            case SUBTYPE_OF: return ElementMatchers.isSubTypeOf(loadClass(m.className));
            default:         throw new IllegalArgumentException("Unknown strategy: " + m.strategy);
        }
    }

    @SuppressWarnings("rawtypes")
    private static ElementMatcher buildMethodMatcher(MethodMatcher m) {
        ElementMatcher.Junction result;
        switch (m.nameStrategy) {
            case EXACT:       result = ElementMatchers.named(m.names.get(0)); break;
            case STARTS_WITH: result = ElementMatchers.nameStartsWith(m.names.get(0)); break;
            case ONE_OF:      result = ElementMatchers.namedOneOf(m.names.toArray(new String[0])); break;
            default:          throw new IllegalArgumentException("Unknown strategy: " + m.nameStrategy);
        }
        if (m.argIndex != null) {
            result = result.and(ElementMatchers.takesArgument(m.argIndex, loadClass(m.argType)));
        }
        if (m.noArgs) {
            result = result.and(ElementMatchers.takesNoArguments());
        }
        return result;
    }

    private static Class<?> loadAdvice(String className, ClassLoader cl) {
        try {
            return Class.forName(className, false, cl);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Advice class not found: " + className, e);
        }
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className, false, ClassLoader.getSystemClassLoader());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found: " + className, e);
        }
    }
}
