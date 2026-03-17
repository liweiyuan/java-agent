package io.github.javaagent.plugin.jdbc;

import net.bytebuddy.asm.Advice;

/**
 * 拦截 Connection.prepareStatement(String sql)，将 SQL 与返回的 PreparedStatement 绑定
 */
public class ConnectionAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(@Advice.Argument(0) String sql,
                              @Advice.Return Object preparedStatement) {
        if (preparedStatement != null) {
            PreparedStatementSqlStore.put(preparedStatement, sql);
        }
    }
}
