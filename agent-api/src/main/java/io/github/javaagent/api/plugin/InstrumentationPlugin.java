package io.github.javaagent.api.plugin;

import net.bytebuddy.agent.builder.AgentBuilder;

/**
 * 插件 SPI 接口，所有插件必须实现此接口
 * 通过 Java SPI 机制加载
 */
public interface InstrumentationPlugin {

    /**
     * 插件名称
     */
    String name();

    /**
     * 向 AgentBuilder 注册字节码增强规则
     */
    AgentBuilder install(AgentBuilder agentBuilder);
}
