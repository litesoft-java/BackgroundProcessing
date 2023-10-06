package org.litesoft.background;

import org.litesoft.annotations.NotNull;
import org.litesoft.pragmatics.ExceptionLogger;
import org.litesoft.exceptionals.ExceptionalRunnable;

/**
 * GracefulShutdownablePulsingRunner combines a <code>Runnable</code> with a
 * <code>ShutdownNowable</code> (<code>GracefulShutdownable</code>) that
 * logs Exceptions with <code>ExceptionLogger</code> implements the shutdown
 * process with a <code>PulsedRunnable</code>.
 */
public class GracefulShutdownablePulsedRunner implements ShutdownNowable,
                                                         Runnable {
    private final ExceptionLogger logger;
    private final PulsedRunnable pulsedRunnable;
    private volatile boolean volatile_shutdown = false;
    private volatile boolean volatile_shutdownRequested = false;
    private volatile Thread volatile_ourThread;

    public GracefulShutdownablePulsedRunner( ExceptionLogger logger, PulsedRunnable pulsedRunnable ) {
        this.logger = NotNull.AssertArgument.namedValue( "logger", logger );
        this.pulsedRunnable = NotNull.AssertArgument.namedValue( "pulsedRunnable", pulsedRunnable );
    }

    public GracefulShutdownablePulsedRunner( ExceptionLogger logger, ExceptionalRunnable pulsedRunnable ) {
        this( logger, PulsedRunnable.from( pulsedRunnable ) );
    }

    @SuppressWarnings("unused")
    public GracefulShutdownablePulsedRunner( ExceptionLogger logger, Runnable pulsedRunnable ) {
        this( logger, PulsedRunnable.from( pulsedRunnable ) );
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
                    prePulsedProcessing();
                    pulsedRunnable.run();
                    postPulsedProcessing();
                }
            }
            catch ( Exception e ) {
                handle( e );
            }
        }
        volatile_shutdown = true;
        volatile_ourThread = null;
    }

    @SuppressWarnings("RedundantThrows")
    protected void prePulsedProcessing() throws Exception {
    }

    @SuppressWarnings("RedundantThrows")
    protected void postPulsedProcessing() throws Exception {
    }

    protected void handle( Exception e ) {
        if ( !(e instanceof InterruptedException) ) {
            logger.log( e );
        }
    }
}
