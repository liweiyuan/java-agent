package io.github.javaagent.core;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;

/**
 * JavaAgent 入口。
 * 职责：将 agent-api 注入 bootstrap，然后用 AgentClassLoader 加载真正的启动类 AgentStarter。
 */
public class AgentBootstrap {

    public static void premain(String agentArgs, Instrumentation inst) throws Exception {
        start(inst);
    }

    public static void agentmain(String agentArgs, Instrumentation inst) throws Exception {
        start(inst);
    }

    private static void start(Instrumentation inst) throws Exception {
        File agentJar = new File(AgentBootstrap.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath());

        inst.appendToBootstrapClassLoaderSearch(new java.util.jar.JarFile(agentJar));

        AgentClassLoader agentCL = new AgentClassLoader(new URL[]{agentJar.toURI().toURL()});
        Class<?> starterClass = agentCL.loadClass("io.github.javaagent.core.AgentStarter");
        Method startMethod = starterClass.getMethod("start", Instrumentation.class, File.class);
        startMethod.invoke(null, inst, agentJar);
    }
}
