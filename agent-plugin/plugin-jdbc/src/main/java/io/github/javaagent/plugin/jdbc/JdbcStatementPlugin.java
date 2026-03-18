package io.github.javaagent.plugin.jdbc;

import io.github.javaagent.api.plugin.InstrumentationPlugin;
import io.github.javaagent.api.plugin.MethodMatcher;
import io.github.javaagent.api.plugin.Transformation;
import io.github.javaagent.api.plugin.TypeMatcher;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

public class JdbcStatementPlugin implements InstrumentationPlugin {

    @Override
    public String name() {
        return "jdbc-statement";
    }

    @Override
    public List<Transformation> transformations() {
        return Arrays.asList(
                // 1. Statement.execute*(String sql)
                Transformation.on(TypeMatcher.subtypeOf(Statement.class))
                        .withAdvice(
                                MethodMatcher.nameStartsWith("execute").withArgument(0, String.class),
                                StatementAdvice.class
                        ),
                // 2. Connection.prepareStatement(String) — 绑定 SQL 与 PreparedStatement
                Transformation.on(TypeMatcher.subtypeOf(Connection.class))
                        .withAdvice(
                                MethodMatcher.named("prepareStatement").withArgument(0, String.class),
                                ConnectionAdvice.class
                        ),
                // 3. PreparedStatement.execute*(无 SQL 参数)
                Transformation.on(TypeMatcher.subtypeOf(PreparedStatement.class))
                        .withAdvice(
                                MethodMatcher.namedOneOf("execute", "executeQuery", "executeUpdate", "executeLargeUpdate").withNoArgs(),
                                PreparedStatementAdvice.class
                        )
        );
    }
}
