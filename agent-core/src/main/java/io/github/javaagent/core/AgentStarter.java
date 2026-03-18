package io.github.javaagent.core;

import io.github.javaagent.api.log.AgentLogger;
import io.github.javaagent.api.log.LoggerFactory;
import io.github.javaagent.api.plugin.InstrumentationPlugin;
import io.github.javaagent.api.trace.GlobalTracer;
import io.github.javaagent.core.config.AgentConfig;
import io.github.javaagent.core.log.LoggingSystem;
import io.github.javaagent.core.trace.DefaultTracer;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.util.ServiceLoader;

/**
 * 真正的启动逻辑，由 AgentClassLoader 加载，与应用 AppClassLoader 隔离。
 */
public class AgentStarter {

    public static void start(Instrumentation inst, File agentJar) {
        File agentDir = agentJar.getParentFile();

        AgentConfig config = AgentConfig.load(new File(agentDir, "agent.yaml"));
        if (config.logging.fileEnabled && !new File(config.logging.filePath).isAbsolute()) {
            config.logging.filePath = new File(agentDir, config.logging.filePath).getAbsolutePath();
        }
        LoggingSystem.init(config.logging);

        final AgentLogger log = LoggerFactory.getLogger(AgentStarter.class);
        log.info("[JavaAgent] Starting...");

        GlobalTracer.set(DefaultTracer.INSTANCE);

        AgentBuilder agentBuilder = new AgentBuilder.Default()
                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
                .with(AgentBuilder.InitializationStrategy.NoOp.INSTANCE)
                .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
                .with(new AgentBuilder.Listener.Adapter() {
                    @Override
                    public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded, Throwable throwable) {
                        log.error("[JavaAgent] Transform error: " + typeName + " - " + throwable.getMessage(), throwable);
                    }
                })
                .ignore(ElementMatchers.nameStartsWith("io.github.javaagent.shaded.")
                        .or(ElementMatchers.nameStartsWith("sun."))
                        .or(ElementMatchers.nameStartsWith("jdk."))
                        .or(ElementMatchers.nameStartsWith("io.github.javaagent.")));

        int count = 0;
        File pluginsDir = new File(agentDir, "plugins");
        if (pluginsDir.isDirectory()) {
            File[] jars = pluginsDir.listFiles(f -> f.getName().endsWith(".jar"));
            if (jars != null) {
                for (File jar : jars) {
                    try {
                        inst.appendToBootstrapClassLoaderSearch(new java.util.jar.JarFile(jar));
                        PluginClassLoader cl = new PluginClassLoader(jar.toURI().toURL(), AgentStarter.class.getClassLoader());
                        for (InstrumentationPlugin plugin : ServiceLoader.load(InstrumentationPlugin.class, cl)) {
                            log.info("[JavaAgent] Installing plugin: " + plugin.name());
                            agentBuilder = PluginInstaller.install(agentBuilder, plugin, cl);
                            count++;
                        }
                    } catch (Exception e) {
                        log.error("[JavaAgent] Failed to load plugin: " + jar.getName(), e);
                    }
                }
            }
        }

        agentBuilder.installOn(inst);
        log.info("[JavaAgent] Started with " + count + " plugin(s).");
    }
}
