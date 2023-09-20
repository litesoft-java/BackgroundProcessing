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

class GracefulShutdownablePulsingRunnerTest implements ExceptionalRunnable,
                                                       ExceptionLogger {


    @Test
    void test_run() {
        ExecutorService executorService =
                Executors.newCachedThreadPool( Thread::new ); // Java 8
        // Executors.newThreadPerTaskExecutor( Thread::new ); // Java 21 Regular Threads
        // Executors.newVirtualThreadPerTaskExecutor(); // Java 21 Virtual Threads

        // executorService.awaitTermination(  )

        GracefulShutdownablePulsingRunner pulsingRunner = new GracefulShutdownablePulsingRunner( this, this );
        assertFalse( pulsingRunner.isShutdown() );
        executorService.execute( pulsingRunner );
        assertFalse( pulsingRunner.isShutdown() );
        sleeper.forMillis( 2 );
        assertFalse( pulsingRunner.isShutdown() );
        assertEquals( 0, exceptions.size() );
        assertFalse( pulsingRunner.isShutdown() );
        runException.set( IO_EXCEPTION );
        sleeper.forMillis( 2 );
        assertFalse( pulsingRunner.isShutdown() );
        assertEquals( 1, exceptions.size() );
        assertSame( IO_EXCEPTION, exceptions.remove(0) );
        sleeper.forMillis( 2 );
        assertFalse( pulsingRunner.isShutdown() );
        assertEquals( 0, exceptions.size() );

        pulsingRunner.shutdownGracefully();
        sleeper.forMillis( 2 );
        assertEquals( 0, exceptions.size() );
        assertTrue( pulsingRunner.isShutdown() );

        pulsingRunner = new GracefulShutdownablePulsingRunner( this, this );
        assertFalse( pulsingRunner.isShutdown() );
        executorService.execute( pulsingRunner );
        assertFalse( pulsingRunner.isShutdown() );
        sleeper.forMillis( 2 );
        assertFalse( pulsingRunner.isShutdown() );

        pulsingRunner.shutdownNow();
        sleeper.forMillis( 2 );
        assertTrue( pulsingRunner.isShutdown() );
    }

    private final AtomicReference<Exception> runException = new AtomicReference<>();

    private final List<Exception> exceptions = new CopyOnWriteArrayList<>();

    @Override
    public void log( Exception e ) {
        exceptions.add( e );
    }

    @Override
    public void run() throws Exception { // pulsingRunnable -- short execution
        Exception e = runException.getAndSet( null );
        if (e != null) {
            throw e;
        }
    }

    private final Sleeper sleeper = Sleeper.INSTANCE;

    public static final IOException IO_EXCEPTION = new IOException();
}