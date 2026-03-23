package io.github.javaagent.api.plugin;

/**
 * 描述目标类的匹配规则，不依赖 Byte Buddy。
 */
public final class TypeMatcher {

    public enum Strategy { NAMED, SUBTYPE_OF }

    public final Strategy strategy;
    public final String className;

    private TypeMatcher(Strategy strategy, String className) {
        this.strategy = strategy;
        this.className = className;
    }

    /** 精确匹配类名，如 "java.net.HttpURLConnection" */
    public static TypeMatcher named(String className) {
        return new TypeMatcher(Strategy.NAMED, className);
    }

    /** 匹配指定类型的所有子类型（含实现类） */
    public static TypeMatcher subtypeOf(String className) {
        return new TypeMatcher(Strategy.SUBTYPE_OF, className);
    }

    /** 匹配指定类型的所有子类型（Class 重载，方便直接传 .class） */
    public static TypeMatcher subtypeOf(Class<?> type) {
        return subtypeOf(type.getName());
    }
}
