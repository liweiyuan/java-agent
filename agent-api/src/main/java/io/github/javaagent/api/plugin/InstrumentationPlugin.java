package io.github.javaagent.api.plugin;

import java.util.List;

/**
 * 插件 SPI 接口。插件只描述增强规则，不依赖 Byte Buddy。
 * 由 agent-core 负责将规则翻译为字节码增强。
 */
public interface InstrumentationPlugin {

    String name();

    /**
     * 插件初始化，在 transformations() 调用前执行。
     * 子类可覆盖此方法读取配置，默认空实现。
     * @param config 扁平化的配置 map，key 格式如 "executor.packages[0]"
     */
    default void init(java.util.Map<String, String> config) {}

    List<Transformation> transformations();
}
