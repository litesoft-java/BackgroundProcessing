package org.litesoft.background;

public interface GracefulShutdownable {
    void shutdownGracefully()
            throws Exception;

    boolean isShutdown();
}
