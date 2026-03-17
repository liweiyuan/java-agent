package io.github.javaagent.plugin.jdbc;

import io.github.javaagent.api.log.AgentLogger;
import io.github.javaagent.api.log.LoggerFactory;
import io.github.javaagent.api.plugin.InstrumentationPlugin;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

public class JdbcStatementPlugin implements InstrumentationPlugin {

    private static final AgentLogger log = LoggerFactory.getLogger(JdbcStatementPlugin.class);

    @Override
    public String name() {
        return "jdbc-statement";
    }

    @Override
    public AgentBuilder install(AgentBuilder agentBuilder) {
        log.debug("[jdbc-plugin] installing...");

        // 1. Statement.execute*(String sql) — 直接从参数取 SQL
        agentBuilder = agentBuilder
                .type(ElementMatchers.isSubTypeOf(java.sql.Statement.class)
                        .and(ElementMatchers.not(ElementMatchers.isSubTypeOf(java.sql.PreparedStatement.class))))
                .transform((builder, type, cl, module, pd) ->
                        builder.visit(Advice.to(StatementAdvice.class)
                                .on(ElementMatchers.nameStartsWith("execute")
                                        .and(ElementMatchers.takesArgument(0, String.class)))));

        // 2. Connection.prepareStatement(String) — 记录 SQL 与 PreparedStatement 的绑定
        agentBuilder = agentBuilder
                .type(ElementMatchers.isSubTypeOf(java.sql.Connection.class))
                .transform((builder, type, cl, module, pd) ->
                        builder.visit(Advice.to(ConnectionAdvice.class)
                                .on(ElementMatchers.named("prepareStatement")
                                        .and(ElementMatchers.takesArgument(0, String.class)))));

        // 3. PreparedStatement.execute/executeQuery/executeUpdate（无 SQL 参数）
        agentBuilder = agentBuilder
                .type(ElementMatchers.isSubTypeOf(java.sql.PreparedStatement.class))
                .transform((builder, type, cl, module, pd) ->
                        builder.visit(Advice.to(PreparedStatementAdvice.class)
                                .on(ElementMatchers.namedOneOf("execute", "executeQuery", "executeUpdate", "executeLargeUpdate")
                                        .and(ElementMatchers.takesNoArguments()
                                                .or(ElementMatchers.not(ElementMatchers.takesArgument(0, String.class)))))));

        return agentBuilder;
    }
}
