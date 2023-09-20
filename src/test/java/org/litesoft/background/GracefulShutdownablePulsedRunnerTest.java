package org.litesoft.background;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.litesoft.pragmatics.ExceptionLogger;
import org.litesoft.utils.ExceptionalRunnable;
import org.litesoft.utils.Sleeper;

import static org.junit.jupiter.api.Assertions.*;

class GracefulShutdownablePulsedRunnerTest implements ExceptionalRunnable,
                                                      ExceptionLogger {
    @Test
    void test_run() {
        ExecutorService executorService =
                Executors.newCachedThreadPool( Thread::new ); // Java 8
        // Executors.newThreadPerTaskExecutor( Thread::new ); // Java 21 Regular Threads
        // Executors.newVirtualThreadPerTaskExecutor(); // Java 21 Virtual Threads

        // executorService.awaitTermination(  )

        GracefulShutdownablePulsedRunner pulsedRunner = new GracefulShutdownablePulsedRunner( this, this );
        assertFalse( pulsedRunner.isShutdown() );
        executorService.execute( pulsedRunner );
        assertFalse( pulsedRunner.isShutdown() );
        sleeper.forMillis( 2 );
        assertFalse( pulsedRunner.isShutdown() );
        assertEquals( 0, exceptions.size() );
        assertFalse( pulsedRunner.isShutdown() );
        runException.set( IO_EXCEPTION );
        sleeper.forMillis( 2 );
        assertFalse( pulsedRunner.isShutdown() );
        assertEquals( 1, exceptions.size() );
        assertSame( IO_EXCEPTION, exceptions.remove( 0 ) );
        sleeper.forMillis( 2 );
        assertFalse( pulsedRunner.isShutdown() );
        assertEquals( 0, exceptions.size() );

        pulsedRunner.shutdownGracefully();
        sleeper.forMillis( 2 );
        assertEquals( 0, exceptions.size() );
        assertTrue( pulsedRunner.isShutdown() );

        pulsedRunner = new GracefulShutdownablePulsedRunner( this, this );
        assertFalse( pulsedRunner.isShutdown() );
        executorService.execute( pulsedRunner );
        assertFalse( pulsedRunner.isShutdown() );
        sleeper.forMillis( 2 );
        assertFalse( pulsedRunner.isShutdown() );

        pulsedRunner.shutdownNow();
        sleeper.forMillis( 2 );
        assertTrue( pulsedRunner.isShutdown() );
    }

    private final AtomicReference<Exception> runException = new AtomicReference<>();

    private final List<Exception> exceptions = new CopyOnWriteArrayList<>();

    @Override
    public void log( Exception e ) {
        exceptions.add( e );
    }

    @Override
    public void run()
            throws Exception { // pulsingRunnable -- short execution
        Exception e = runException.getAndSet( null );
        if ( e != null ) {
            throw e;
        }
    }

    private final Sleeper sleeper = Sleeper.INSTANCE;

    public static final IOException IO_EXCEPTION = new IOException();
}