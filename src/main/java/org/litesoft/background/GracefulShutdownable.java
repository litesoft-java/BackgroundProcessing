package org.litesoft.background;

/**
 * GracefulShutdownable interface that supports <code>isShutdown</code> and
 * <code>shutdownGracefully</code>
 */
public interface GracefulShutdownable {
    void shutdownGracefully()
            throws Exception;

    boolean isShutdown();
}
