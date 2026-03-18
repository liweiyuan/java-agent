package io.github.javaagent.api.plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 描述目标方法的匹配规则，支持链式组合，不依赖 Byte Buddy。
 */
public final class MethodMatcher {

    public enum NameStrategy { EXACT, STARTS_WITH, ONE_OF }

    public final NameStrategy nameStrategy;
    public final List<String> names;

    /** null = 不限制参数；非 null = 第 index 个参数必须是指定类型 */
    public final Integer argIndex;
    public final String argType;

    /** true = 要求无参数 */
    public final boolean noArgs;

    private MethodMatcher(NameStrategy nameStrategy, List<String> names,
                          Integer argIndex, String argType, boolean noArgs) {
        this.nameStrategy = nameStrategy;
        this.names = names;
        this.argIndex = argIndex;
        this.argType = argType;
        this.noArgs = noArgs;
    }

    public static MethodMatcher named(String name) {
        return new MethodMatcher(NameStrategy.EXACT, Collections.singletonList(name), null, null, false);
    }

    public static MethodMatcher nameStartsWith(String prefix) {
        return new MethodMatcher(NameStrategy.STARTS_WITH, Collections.singletonList(prefix), null, null, false);
    }

    public static MethodMatcher namedOneOf(String... names) {
        return new MethodMatcher(NameStrategy.ONE_OF, Arrays.asList(names), null, null, false);
    }

    /** 追加"第 index 个参数类型为 argType"的约束 */
    public MethodMatcher withArgument(int index, Class<?> type) {
        return new MethodMatcher(nameStrategy, names, index, type.getName(), noArgs);
    }

    /** 追加"无参数"约束 */
    public MethodMatcher withNoArgs() {
        return new MethodMatcher(nameStrategy, names, argIndex, argType, true);
    }
}
