package io.github.javaagent.api.plugin;

import java.util.List;

/**
 * 插件 SPI 接口。插件只描述增强规则，不依赖 Byte Buddy。
 * 由 agent-core 负责将规则翻译为字节码增强。
 */
public interface InstrumentationPlugin {

    String name();

    List<Transformation> transformations();
}
