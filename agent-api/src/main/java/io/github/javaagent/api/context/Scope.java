package io.github.javaagent.api.context;

import java.io.Closeable;

/**
 * Context 的作用域，关闭时恢复上一个 Context
 */
public interface Scope extends Closeable {

    @Override
    void close();
}
