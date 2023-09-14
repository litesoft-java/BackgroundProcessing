package org.litesoft.utils;

/**
 * Similar to a <code>Callable<Void></code> but simpler to use.
 */
@SuppressWarnings("unused")
public interface ExceptionalRunnable {
    void run()
            throws Exception;
}
