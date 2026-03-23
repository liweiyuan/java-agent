package io.github.javaagent.core;

import java.net.URL;
import java.net.URLClassLoader;

/** 每个插件 jar 独立的 ClassLoader，parent 指向 AgentClassLoader，插件间互相隔离。 */
public class PluginClassLoader extends URLClassLoader {

    static {
        ClassLoader.registerAsParallelCapable();
    }

    public PluginClassLoader(URL jarUrl, ClassLoader agentClassLoader) {
        super(new URL[]{jarUrl}, agentClassLoader);
    }
}
