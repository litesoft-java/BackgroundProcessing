package org.litesoft.utils;

import java.util.function.LongSupplier;

import org.litesoft.pragmatics.ExceptionHandler;
import org.litesoft.pragmatics.Exceptions;

public class Sleeper {
    @SuppressWarnings("unused")
    public static final Sleeper INSTANCE = new Sleeper( System::currentTimeMillis, Thread::sleep );

    private final LongSupplier millisTimeSource;
    private final ExceptionalLongConsumer sleepMethod;

    public Sleeper( LongSupplier millisTimeSource, ExceptionalLongConsumer sleepMethod ) {
        this.millisTimeSource = millisTimeSource;
        this.sleepMethod = sleepMethod;
    }

    public void forMillis( int millis ) {
        long now = millisTimeSource.getAsLong();
        long tillTime = now + millis;
        while ( tillTime > now ) {
            try {
                sleepMethod.accept( tillTime - now );
            }
            catch ( InterruptedException e ) {
                Exceptions.swallowExpected( e );
            }
            catch ( Exception e ) {
                ExceptionHandler.propagate().handle( e );
            }
            now = millisTimeSource.getAsLong();
        }
    }
}
