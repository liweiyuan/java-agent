package io.github.javaagent.api.context;

import java.util.WeakHashMap;

/** 存储 new Thread 场景下捕获的 Context，WeakHashMap 保证不阻止线程 GC。 */
public final class ThreadContextRegistry {

    private static final WeakHashMap<Thread, Context> MAP = new WeakHashMap<Thread, Context>();

    private ThreadContextRegistry() {}

    public static synchronized void capture(Thread thread, Context context) {
        if (context != null && context != Context.root()) {
            MAP.put(thread, context);
        }
    }

    public static synchronized Context get(Thread thread) {
        return MAP.get(thread);
    }

    public static synchronized void remove(Thread thread) {
        MAP.remove(thread);
    }
}
