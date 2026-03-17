package io.github.javaagent.plugin.http;

import io.github.javaagent.api.log.AgentLogger;
import io.github.javaagent.api.log.LoggerFactory;
import io.github.javaagent.api.plugin.InstrumentationPlugin;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.matcher.ElementMatchers;

public class HttpUrlConnectionPlugin implements InstrumentationPlugin {

    private static final AgentLogger log = LoggerFactory.getLogger(HttpUrlConnectionPlugin.class);

    @Override
    public String name() {
        return "http-urlconnection";
    }

    @Override
    public AgentBuilder install(AgentBuilder agentBuilder) {
        log.debug("[http-plugin] installing...");
        return agentBuilder
                .type(ElementMatchers.named("java.net.HttpURLConnection")
                        .or(ElementMatchers.isSubTypeOf(java.net.HttpURLConnection.class)))
                .transform((builder, typeDescription, classLoader, module, protectionDomain) ->
                        builder.visit(Advice.to(HttpUrlConnectionAdvice.class)
                                .on(ElementMatchers.named("getResponseCode"))));
    }
}
