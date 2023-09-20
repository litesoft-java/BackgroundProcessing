package org.litesoft.background;

import org.litesoft.annotations.NotNull;
import org.litesoft.pragmatics.ExceptionLogger;
import org.litesoft.utils.ExceptionalRunnable;

public class GracefulShutdownablePulsingRunner implements ShutdownNowable,
                                                          Runnable {
    private final ExceptionLogger logger;
    private final PulsingRunnable pulsingRunnable;
    private volatile boolean volatile_shutdown = false;
    private volatile boolean volatile_shutdownRequested = false;
    private volatile Thread volatile_ourThread;

    public GracefulShutdownablePulsingRunner( ExceptionLogger logger, PulsingRunnable pulsingRunnable ) {
        this.logger = NotNull.AssertArgument.namedValue( "logger", logger );
        this.pulsingRunnable = NotNull.AssertArgument.namedValue( "pulsingRunnable", pulsingRunnable );
    }

    public GracefulShutdownablePulsingRunner( ExceptionLogger logger, ExceptionalRunnable pulsingRunnable ) {
        this(logger, PulsingRunnable.from( pulsingRunnable ) );
    }

    @SuppressWarnings("unused")
    public GracefulShutdownablePulsingRunner( ExceptionLogger logger, Runnable pulsingRunnable ) {
        this(logger, PulsingRunnable.from( pulsingRunnable ) );
    }

    @Override
    public boolean isShutdown() {
        return volatile_shutdown;
    }

    @Override
    public void shutdownGracefully() {
        volatile_shutdownRequested = true;
    }

    @Override
    public void shutdownNow() {
        volatile_shutdownRequested = true;
        Thread ourThread = volatile_ourThread; // seize current value
        if ( !isShutdown() && (ourThread != null) ) {
            ourThread.interrupt();
        }
    }

    @Override
    public void run() {
        volatile_ourThread = Thread.currentThread();
        while ( !volatile_shutdownRequested ) {
            try {
                if ( !Thread.interrupted() ) {
                    pulsingRunnable.run();
                }
            }
            catch ( Exception e ) {
                handle( e );
            }
        }
        volatile_shutdown = true;
        volatile_ourThread = null;
    }

    protected void handle( Exception e ) {
        if ( !(e instanceof InterruptedException) ) {
            logger.log( e );
        }
    }
}
