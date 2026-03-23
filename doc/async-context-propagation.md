# 异步任务上下文传播方案

## 问题本质

当前项目的 `Context` 基于 `ThreadLocal`，天然只在当前线程有效。

```
主线程（Tomcat worker）
  └── Span A（traceId=xxx, spanId=1）存在 ThreadLocal 里
        │
        ├── 提交异步任务到线程池
        │     └── 子线程：ThreadLocal 是空的！traceId 丢失
        └── 主线程继续...
```

核心问题：**如何在提交异步任务时，把当前 Context 传递到子线程？**

---

## 方案：增强线程池的 `execute` / `submit`

在任务提交时**捕获当前 Context，包装 Runnable/Callable**，在子线程执行前恢复。

### 两个切入点对比

| 方案 | 切入点 | 适用场景 |
|---|---|---|
| 方案 A（推荐） | 增强 `ExecutorService.execute()` / `submit()`，包装任务 | 线程池复用场景 |
| 方案 B | 增强 `Thread.start()`，传递 Context | 粒度粗，不适合线程池 |

---

## 推荐方案：增强 `ThreadPoolExecutor.execute()` + 包装 Runnable

### 流程

```
主线程
  Advice.onEnter(execute(Runnable r))
    capturedCtx = Context.current()                              // 捕获当前 Span
    args[0] = new ContextPropagatingRunnable(r, capturedCtx)    // 替换参数

子线程
  ContextPropagatingRunnable.run()
    try (Scope s = capturedCtx.makeCurrent()) {  // 恢复 Context
        originalRunnable.run()                    // 执行原始任务
    }                                             // Scope.close() 弹栈
```

### 关键实现点

1. **Advice 替换参数**：使用 Byte Buddy 的 `@Advice.Argument(value=0, readOnly=false)` 直接替换方法参数，把原始 `Runnable` 换成包装版本。

2. **`ContextPropagatingRunnable` 放在 `agent-api`**：需要被 Bootstrap ClassLoader 加载，才能在任意线程中访问。

3. **避免重复包装**：如果已经是 `ContextPropagatingRunnable` 则跳过，防止嵌套提交时多次包装。

4. **`Callable` 同理**：`submit(Callable)` 也需要同样处理。

---

## 需要增强的类（线程池场景）

| 类 | 方法 | 说明 |
|---|---|---|
| `java.util.concurrent.ThreadPoolExecutor` | `execute(Runnable)` | 最底层，覆盖大多数场景 |
| `java.util.concurrent.AbstractExecutorService` | `submit(Runnable)` / `submit(Callable)` | 最终也会走 execute |
| `java.util.concurrent.ScheduledThreadPoolExecutor` | `schedule(...)` | 定时任务场景 |

> 实际上只增强 `ThreadPoolExecutor.execute()` 就能覆盖绝大多数场景，因为 `submit` 内部会调用 `execute`。

---

## `new Thread` 场景

线程池方案无法覆盖 `new Thread(runnable).start()` 的用法，需要单独处理。

### 切入点

增强 `Thread` 的构造方法捕获 Context，在 `Thread.run()` 时恢复：

```
主线程
  Thread.<init>(Runnable r)  onEnter:
    capturedCtx = Context.current()
    ThreadContextRegistry.capture(this, capturedCtx)   // 存入全局 WeakHashMap

子线程
  Thread.run()  onEnter:
    ctx = ThreadContextRegistry.get(currentThread())
    scope = ctx.makeCurrent()                          // 恢复 Context

  Thread.run()  onExit:
    scope.close()
    ThreadContextRegistry.remove(currentThread())      // 清理
```

### 存储方案

用 `WeakHashMap<Thread, Context>` 存放捕获的 Context，放在 `agent-api` 中（Bootstrap ClassLoader 加载）：

- key 是 `Thread` 实例，`WeakHashMap` 保证不阻止 GC
- 线程结束后自动被回收，无内存泄漏风险

---

## 完整覆盖策略

| 场景 | 增强点 |
|---|---|
| 线程池 | `ThreadPoolExecutor.execute()` 包装 Runnable |
| `new Thread` | `Thread.<init>` 捕获 + `Thread.run()` 恢复 |
| `ForkJoinPool`（含 `CompletableFuture`） | `ForkJoinTask.exec()` 或包装 `ForkJoinTask` |

> `CompletableFuture` 默认使用 `ForkJoinPool.commonPool()`，如需覆盖该场景需单独处理，优先级可放后面。

---

## 对应到当前项目的实现步骤

1. 在 `agent-api` 里添加 `ContextPropagatingRunnable`、`ContextPropagatingCallable`、`ThreadContextRegistry`
2. 新建 `agent-plugin/plugin-executor` 模块
3. 实现 `ExecutorPlugin`，增强 `ThreadPoolExecutor.execute()`（包装 Runnable）
4. 实现 `ThreadPlugin`，增强 `Thread.<init>` 和 `Thread.run()`
5. 在 `agent-bootstrap/pom.xml` 中添加依赖，执行 `./build.sh`

这样，从 Tomcat 进来的请求，无论通过线程池还是 `new Thread` 派生异步任务，都会自动继承主线程的 `traceId`，形成完整的调用链。

---

## 已加载类的 retransform 问题

### 问题

`ThreadPoolExecutor` 和 `Thread` 都是 JDK 核心类，JVM 启动时就已被 Bootstrap ClassLoader 加载。Byte Buddy 默认只处理**之后新加载的类**，对已加载的类不生效。

### 当前配置

`AgentStarter` 中已配置：

```java
new AgentBuilder.Default()
    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
    .with(AgentBuilder.TypeStrategy.Default.REDEFINE)
```

`RedefinitionStrategy.RETRANSFORMATION` 会在 `installOn(inst)` 时自动对已加载的匹配类触发 `retransformClasses`，**不需要手动调用**。

### 生效的前提

插件规则必须在 `installOn` 之前全部注册完毕。当前流程满足这一条件：

```
扫描 plugins/ → 注册所有插件规则到 agentBuilder → installOn(inst)
                                                      └── 自动 retransform 已加载的匹配类
```

### TypeStrategy 与 RedefinitionStrategy 组合

| TypeStrategy | RedefinitionStrategy | 效果 |
|---|---|---|
| `REDEFINE` | `RETRANSFORMATION` | 对已加载类触发 retransform ✅ |
| `DECORATE` | `RETRANSFORMATION` | 同上，但保留原始方法 |

当前 `REDEFINE + RETRANSFORMATION` 组合对 `Thread`、`ThreadPoolExecutor` 等已加载的 JDK 类**会生效**，无需额外处理。
