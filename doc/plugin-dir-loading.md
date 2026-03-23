# 插件目录加载机制设计

## 背景

当前插件（plugin-http、plugin-jdbc）打包在 agent fat jar 内部，由 `AgentClassLoader` 统一加载。
若要支持插件从外部 `plugins/` 目录动态加载，需要解决以下问题。

---

## 需要补充的机制

### 1. 插件 ClassLoader 隔离

每个插件 jar 需要独立的 `PluginClassLoader`，parent 指向 `AgentClassLoader`：

```
Bootstrap ClassLoader          ← agent-api（全局唯一）
    └── AgentClassLoader       ← agent-core（隔离于应用）
            ├── PluginClassLoader-http   ← plugin-http.jar
            └── PluginClassLoader-jdbc   ← plugin-jdbc.jar
```

**好处**：
- 插件间互相隔离，不同插件可依赖同一库的不同版本
- 插件能访问 agent-core（parent）和 agent-api（Bootstrap）
- 应用代码无法访问插件内部实现

**实现**：

```java
for (File jar : pluginsDir.listFiles()) {
    PluginClassLoader cl = new PluginClassLoader(
        jar.getName(),
        new URL[]{jar.toURI().toURL()},
        agentClassLoader   // parent
    );
    // 用此 ClassLoader 执行 ServiceLoader 发现插件
    ServiceLoader.load(InstrumentationPlugin.class, cl);
}
```

---

### 2. Advice 类的可见性（最关键）

Byte Buddy `Advice` 通过**字节码内联**工作：`@OnMethodEnter`/`@OnMethodExit` 的内容被直接复制到目标类方法里。目标类（如 Hibernate `StatementImpl`）由 AppClassLoader 加载，内联后它的字节码会直接引用 Advice 里用到的类，这些类必须对 AppClassLoader 可见。

**解法**：将插件 jar 注入 Bootstrap ClassLoader：

```java
inst.appendToBootstrapClassLoaderSearch(new JarFile(pluginJar));
```

**约束**：注入 Bootstrap 后，Advice 类只能引用 Bootstrap 可见的类，即只能引用 **agent-api** 的接口（`GlobalTracer`、`LoggerFactory` 等），**不能直接引用 agent-core 的实现类**。

这正是我们将 `DefaultTracer.INSTANCE` 替换为 `GlobalTracer.get()` 的根本原因，当前代码已满足此约束。

---

### 3. SPI 发现机制

拆分后每个插件有独立 ClassLoader，需要逐个发现：

```java
List<InstrumentationPlugin> plugins = new ArrayList<>();
for (File jar : pluginsDir.listFiles()) {
    // 1. 注入 Bootstrap（Advice 可见性）
    inst.appendToBootstrapClassLoaderSearch(new JarFile(jar));

    // 2. 创建独立 PluginClassLoader
    PluginClassLoader cl = new PluginClassLoader(jar.getName(),
            new URL[]{jar.toURI().toURL()}, agentClassLoader);

    // 3. ServiceLoader 发现此 jar 内的插件
    for (InstrumentationPlugin p : ServiceLoader.load(InstrumentationPlugin.class, cl)) {
        plugins.add(p);
    }
}
```

---

### 4. 插件依赖管理

插件 jar 如果有第三方依赖，有两种方案：

| 方案 | 优点 | 缺点 |
|---|---|---|
| 插件打成 fat jar（shade） | 简单，自包含 | jar 体积大，依赖重复 |
| 插件目录下放 lib/ 子目录 | 依赖共享，体积小 | 需要额外的依赖扫描逻辑 |

推荐方案：插件打成 fat jar，shade 时排除 agent-api 和 agent-core（scope=provided），只打入插件自身的第三方依赖。

---

### 5. 热加载（可选，较复杂）

若需运行时动态加载插件（不重启 JVM）：

- 通过 `agentmain` + `VirtualMachine.attach()` 触发
- 新插件加载后，对已加载的目标类调用 `inst.retransformClasses()` 重新增强
- 卸载插件时需撤销增强，Byte Buddy 支持通过 `AgentBuilder.with(RedefinitionStrategy.RETRANSFORMATION)` 还原

当前阶段不需要实现，记录为未来扩展点。

---

## 当前代码已满足的前提条件

| 条件 | 状态 |
|---|---|
| Advice 只引用 agent-api（`GlobalTracer.get()`、`LoggerFactory`） | ✅ 已满足 |
| agent-api 注入 Bootstrap，全局唯一 | ✅ 已满足 |
| AgentClassLoader 与 AppClassLoader 隔离 | ✅ 已满足 |
| 插件编译时 agent-api 为 compile，agent-core 为 provided | ✅ 已满足 |

满足以上前提后，实现插件目录加载只需：
1. 新增 `PluginClassLoader`
2. 修改 `AgentStarter` 的插件加载逻辑（遍历目录 → 创建 PluginClassLoader → ServiceLoader）
3. `build.sh` 将插件 jar 输出到 `output/plugins/` 而非打入 fat jar
