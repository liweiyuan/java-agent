package io.github.javaagent.plugin.jdbc;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 存储 PreparedStatement 实例 -> SQL 的映射，使用 WeakHashMap 避免内存泄漏
 */
public final class PreparedStatementSqlStore {

    private static final Map<Object, String> STORE = new WeakHashMap<>();

    public static synchronized void put(Object stmt, String sql) {
        STORE.put(stmt, sql);
    }

    public static synchronized String get(Object stmt) {
        return STORE.get(stmt);
    }
}
