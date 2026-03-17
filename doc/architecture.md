# JavaAgent 架构设计文档

## 项目结构

```
java-agent/
├── agent-api/          # 公共接口层（Context、Span、Tracer、Logger SPI）
├── agent-core/         # 核心实现（AgentBootstrap、DefaultTracer、LoggingSystem）
├── agent-bootstrap/    # 最终打包模块（shade + relocate，产出 fat jar）
├── agent-plugin/
│   ├── plugin-http/    # HttpURLConnection 插件
│   └── plugin-jdbc/    # JDBC Statement/PreparedStatement 插件
├── output/             # 构建产物
│   ├── java-agent.jar
│   └── agent.yaml
└── build.sh            # 一键构建脚本
```

---

## ClassLoader 隔离架构

这是整个系统最核心的设计，参考 OpenTelemetry Java Agent 的实现。

```
Bootstrap ClassLoader
  └── agent-api.*
      ├── LoggerFactory      ← 全局唯一，factory 由 AgentStarter 注入
      ├── GlobalTracer       ← 全局唯一，instance 由 AgentStarter 注入
      ├── Context / Scope    ← Advice 内联后对任意 ClassLoader 可见
      └── InstrumentationPlugin (SPI 接口)

AgentClassLoader  (parent = Bootstrap)
  └── agent-core.*
      ├── AgentStarter       ← 真正的启动逻辑
      ├── DefaultTracer      ← Tracer 实现
      ├── SpanExporter       ← Span 输出
      └── LoggingSystem      ← 日志系统初始化
  └── agent-plugin.*
      ├── Advice 类          ← 字节码内联，只引用 agent-api 接口
      └── Plugin 实现        ← 注册 Byte Buddy 增强规则

AppClassLoader  (parent = Bootstrap，Spring/应用代码)
  └── 加载 LoggerFactory → 委托 Bootstrap → 拿到同一份类
      factory 已由 AgentStarter 注入 → 日志正常写入文件 ✅
```

### 为什么需要这样设计

**问题：静态字段隔离**

`LoggerFactory.factory` 是静态字段。如果 `LoggerFactory` 被 Bootstrap 和 AppClassLoader 各加载一份，就是两个独立的类，静态字段互不影响。`AgentStarter` 向 Bootstrap 里的 `LoggerFactory` 注入了 `factory`，但 `SpanExporter` 被 AppClassLoader 加载，拿到的是 AppClassLoader 里的 `LoggerFactory`，其 `factory` 为 `null`，走 `NoopLogger`，日志被 Spring/Logback 接管。

**解法：agent-api 注入 Bootstrap，全局唯一**

```java
// AgentBootstrap.start()
inst.appendToBootstrapClassLoaderSearch(new JarFile(agentJar));
```

fat jar 注入 Bootstrap 后，任何 ClassLoader 加载 `LoggerFactory` 时都会委托 Bootstrap，拿到同一份类和同一个静态字段。

**问题：Byte Buddy 类冲突**

插件 jar 和 agent-core fat jar 都包含 `net.bytebuddy.*`，AppClassLoader 和 AgentClassLoader 各加载一份，`SubTypeMatcher` 实现的接口来自不同版本，抛 `IncompatibleClassChangeError`。

**解法：shade + relocate**

`agent-bootstrap` 模块用 maven-shade-plugin 将所有 Byte Buddy 类重命名：

```
net.bytebuddy.** → io.github.javaagent.shaded.net.bytebuddy.**
```

fat jar 里只有 shaded 版本，与应用自身依赖的 `net.bytebuddy` 完全隔离。

---

## 启动流程

```
JVM 调用 AgentBootstrap.premain()
  │
  ├─ 1. 将 fat jar 注入 Bootstrap ClassLoader
  │      inst.appendToBootstrapClassLoaderSearch(agentJar)
  │      → agent-api 类全局唯一
  │
  ├─ 2. 创建 AgentClassLoader，加载 AgentStarter
  │      AgentClassLoader.loadClass("AgentStarter")
  │      → agent-core/plugin 与应用隔离
  │
  └─ 3. 反射调用 AgentStarter.start()
         ├─ 读取 agent.yaml，初始化 LoggingSystem
         ├─ LoggerFactory.setFactory(...)  → 注入到 Bootstrap 的静态字段
         ├─ GlobalTracer.set(DefaultTracer.INSTANCE)
         ├─ 构建 AgentBuilder（Byte Buddy，shaded 版本）
         ├─ ServiceLoader 加载所有 InstrumentationPlugin
         └─ agentBuilder.installOn(inst)
```

---

## 上下文传递（参考 OpenTelemetry）

```
请求进入
  │
  ├─ Advice.onEnter()
  │    Tracer.spanBuilder("DB Query").start()
  │      → 从 Context.current() 取父 Span（ThreadLocal）
  │      → 创建子 Span，继承 traceId
  │      → 新 Context 设为当前线程活跃（返回 Scope）
  │
  ├─ 执行原始方法
  │
  └─ Advice.onExit()
       span.end()       → 导出 Span
       scope.close()    → 恢复上一个 Context（ThreadLocal 弹栈）
```

`Context` 不可变，`with()` 返回新实例，`Scope` 用 try-with-resources 保证 ThreadLocal 恢复，天然支持嵌套调用链。

---

## 日志系统

### 配置文件 agent.yaml

```yaml
logging:
  level: INFO          # DEBUG / INFO / WARN / ERROR
  console:
    enabled: true
  file:
    enabled: true
    path: logs/agent.log   # 相对路径基于 agent jar 所在目录
    size-mb: 10            # 单文件最大大小
    max-backups: 5         # 最多保留历史文件数
```

### 滚动策略

- 文件大小超过 `size-mb` 时触发滚动
- 历史文件命名：`agent.log.1`、`agent.log.2` ... `agent.log.N`
- 超过 `max-backups` 的最老文件自动删除

### 插件中使用日志

```java
import io.github.javaagent.api.log.AgentLogger;
import io.github.javaagent.api.log.LoggerFactory;

private static final AgentLogger log = LoggerFactory.getLogger(MyPlugin.class);

log.debug("...");
log.info("...");
log.error("...", throwable);
```

`LoggerFactory.getLogger()` 返回 `DelegatingLogger`，每次调用动态委托给当前 `factory`，避免静态字段缓存 `NoopLogger` 导致日志系统初始化前后行为不一致。

---

## 新增插件

1. 在 `agent-plugin/` 下新建 maven 模块
2. 依赖 `agent-api`（compile）、`agent-core`（provided）
3. 实现 `InstrumentationPlugin` 接口
4. 注册 SPI：`META-INF/services/io.github.javaagent.api.plugin.InstrumentationPlugin`
5. 在 `agent-bootstrap/pom.xml` 中添加依赖
6. 执行 `./build.sh`
