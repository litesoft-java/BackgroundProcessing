package org.litesoft.background;

/**
 * ShutdownNowable is a <code>GracefulShutdownable</code> that adds a <code>shutdownNow</code> method.
 * <p>
 * The <code>GracefulShutdownManager</code> processes all the "regular" <code>GracefulShutdownable</code>(s), then processes all the <code>ShutdownNowable</code>(s).
 */
public interface ShutdownNowable extends GracefulShutdownable {
    void shutdownNow()
            throws Exception;
}
