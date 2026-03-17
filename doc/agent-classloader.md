# AgentClassLoader 详解

## 构造：parent = null

```java
super(urls, null);
```

标准双亲委派中，parent 通常是 AppClassLoader。这里设为 `null`，意味着 **parent 是 Bootstrap ClassLoader**。AgentClassLoader 与 AppClassLoader 完全平行，互不干扰。

```
Bootstrap ClassLoader
    ├── AgentClassLoader  (parent=Bootstrap)
    └── AppClassLoader    (parent=Bootstrap 的 ExtClassLoader)
```

---

## loadClass 逐段解析

### 第一段：缓存检查

```java
Class<?> c = findLoadedClass(name);
if (c != null) return c;
```

JVM 保证同一个 ClassLoader 不会重复加载同名类。先查缓存，命中直接返回。

---

### 第二段：agent-api 强制走 Bootstrap

```java
if (name.startsWith("io.github.javaagent.api.")
        || name.equals("io.github.javaagent.core.AgentBootstrap")
        || name.equals("io.github.javaagent.core.AgentClassLoader")) {
    return Class.forName(name, false, null);
}
```

`Class.forName(name, false, null)` 第三个参数 `null` 表示用 **Bootstrap ClassLoader** 加载。

**为什么 agent-api 必须走 Bootstrap？**

`LoggerFactory.factory` 和 `GlobalTracer.instance` 是静态字段。如果这两个类被多个 ClassLoader 各加载一份，每份都有独立的静态字段，`AgentStarter` 向其中一份注入了 `factory`，另一份的 `factory` 仍是 `null`。

强制走 Bootstrap 后，全局只有一份 `LoggerFactory` 类，所有 ClassLoader 加载它时都委托给 Bootstrap，共享同一个静态字段。

**为什么 AgentBootstrap/AgentClassLoader 自身也要走 Bootstrap？**

这两个类在 premain 阶段已由 AppClassLoader 加载。如果 AgentClassLoader 再从自身 jar 加载一份，反射调用时会出现类型不匹配。强制走 Bootstrap（实际拿到的是已加载的版本）保证唯一性。

---

### 第三段：agent 内部类从自身加载（隔离）

```java
if (name.startsWith("io.github.javaagent.")) {
    c = findClass(name);  // 从 fat jar 里找，不委托 parent
    if (resolve) resolveClass(c);
    return c;
}
```

`findClass` 直接从构造时传入的 `urls`（fat jar）里加载，**不委托给 parent**，这是打破双亲委派的关键。

效果：
- `AgentStarter`、`DefaultTracer`、`SpanExporter` 等只存在于 AgentClassLoader 里
- AppClassLoader 里没有这些类，应用代码无法直接引用 agent 内部实现
- 实现 **agent 实现与应用的双向隔离**

---

### 第四段：其他类走 Bootstrap → System

```java
try {
    return Class.forName(name, false, null);  // Bootstrap
} catch (ClassNotFoundException ignored) {}

return getSystemClassLoader().loadClass(name);  // AppClassLoader
```

JDK 类（`java.*`、`sun.*`）Bootstrap 能找到直接返回。找不到的走 System ClassLoader 兜底。

---

## 与标准双亲委派的对比

| | 标准双亲委派 | AgentClassLoader |
|---|---|---|
| 加载顺序 | parent → 自己 | 按包名分流 |
| agent-api | AppClassLoader 加载（多份） | Bootstrap 加载（唯一） |
| agent-core/plugin | AppClassLoader 加载（暴露给应用） | AgentClassLoader 加载（隔离） |
| JDK 类 | Bootstrap | Bootstrap（相同） |

---

## 整体效果

```
LoggerFactory（Bootstrap，唯一）
    ↑ 所有 ClassLoader 加载它都拿到同一份

AgentStarter 调用 LoggerFactory.setFactory(impl)
    → 注入到唯一的静态字段

SpanExporter（AgentClassLoader）调用 LoggerFactory.getLogger()
    → 委托 Bootstrap → 同一份 LoggerFactory → factory 已注入 → 正常写入日志文件 ✅

Spring 代码（AppClassLoader）调用 LoggerFactory.getLogger()
    → 委托 Bootstrap → 同一份 LoggerFactory → factory 已注入 → 正常写入日志文件 ✅
```
