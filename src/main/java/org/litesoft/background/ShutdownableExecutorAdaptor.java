package org.litesoft.background;

import java.util.concurrent.ExecutorService;

import org.litesoft.annotations.NotNull;
import org.litesoft.exceptionals.ExceptionalRunnable;
import org.litesoft.pragmatics.ExceptionLogger;

/**
 * ShutdownableExecutorAdaptor adapts an <code>ExecutorService</code> into a
 * <code>ShutdownNowable</code> (<code>GracefulShutdownable</code>) that
 * can be set as the <code>GracefulShutdownManager</code>'s <code>lastNowable</code>.
 */
@SuppressWarnings("unused")
public class ShutdownableExecutorAdaptor implements ShutdownNowable {
    private final ExecutorService executor;

    public ShutdownableExecutorAdaptor( ExecutorService executor ) {
        this.executor = NotNull.AssertArgument.namedValue( "executor", executor );
    }

    @Override
    public boolean isShutdown() {
        return executor.isShutdown();
    }

    @Override
    public void shutdownGracefully() {
        executor.shutdown();
    }

    @Override
    public void shutdownNow() {
        executor.shutdownNow();
    }
}
