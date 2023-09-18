package org.litesoft.background;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.function.LongSupplier;

import org.litesoft.annotations.NotNull;
import org.litesoft.utils.ExceptionalConsumer;
import org.litesoft.utils.ExceptionalLongConsumer;
import org.litesoft.utils.Sleeper;

@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
public class GracefulShutdownManager {
    public static final int DEFAULT_GRACE_SECS = 10;
    public static final int MINIMUM_GRACE_SECS_ACCEPTABLE = 2;

    private final List<GracefulShutdownable> regulars = new ArrayList<>();
    private final List<ShutdownNowable> nowables = new ArrayList<>();
    private final ShutdownNowable lastNowable;
    private final Executor executor;
    private final LongSupplier millisTimeSource;
    private final Sleeper sleeper;

    private int graceSeconds = DEFAULT_GRACE_SECS;

    public GracefulShutdownManager maxGraceSeconds( int seconds ) {
        if ( MINIMUM_GRACE_SECS_ACCEPTABLE <= seconds ) {
            graceSeconds = seconds;
        }
        return this;
    }

    public GracefulShutdownManager add( GracefulShutdownable... shutdownables ) {
        return (shutdownables == null) ? this : add( Arrays.asList( shutdownables ) );
    }

    public GracefulShutdownManager add( List<GracefulShutdownable> shutdownables ) {
        if ( shutdownables != null ) {
            for ( GracefulShutdownable shutdownable : shutdownables ) {
                if ( shutdownable != null ) {
                    if ( shutdownable instanceof ShutdownNowable ) {
                        append( nowables, (ShutdownNowable)shutdownable );
                    } else {
                        append( regulars, shutdownable );
                    }
                    if ( shutdownable instanceof Runnable ) {
                        executor.execute( (Runnable)shutdownable );
                    }
                }
            }
        }
        return this;
    }

    public List<Exception> shutdownGracefully() {
        List<Exception> problems = new ArrayList<>();

        long timesUpMillis = millisTimeSource.getAsLong() + (graceSeconds * 1000L);

        gracefully( nowables, problems ); // first so get a bit more time
        gracefully( regulars, problems );

        while ( anyRemainingNotDone() ) {
            long remainingMillis = timesUpMillis - millisTimeSource.getAsLong();
            if ( remainingMillis <= 0 ) {
                return now( nowables, problems, lastNowable );
            }
            if ( remainingMillis > 2 ) {
                sleeper.forMillis( 2 );
            }
        }
        return problems; // everything is Done!
    }

    public List<Exception> shutdownNow() {
        return now( nowables, new ArrayList<>(), lastNowable );
    }

    public GracefulShutdownManager( Executor executor, ShutdownNowable lastNowable ) {
        this( executor, lastNowable, System::currentTimeMillis, Thread::sleep );
    }

    protected GracefulShutdownManager( Executor executor, ShutdownNowable lastNowable,
                                       LongSupplier millisTimeSource, ExceptionalLongConsumer sleepMethod ) {
        this.executor = NotNull.AssertError.namedValue( "executor", executor );
        this.lastNowable = lastNowable;
        this.millisTimeSource = millisTimeSource;
        sleeper = new Sleeper( millisTimeSource, sleepMethod );
    }

    protected GracefulShutdownManager resetForTests() {
        clear( regulars );
        clear( nowables );
        graceSeconds = 10;
        return this;
    }

    protected boolean anyRemainingNotDone() {
        boolean someNows = !purgeDone( nowables );
        boolean someRegs = !purgeDone( regulars );
        return someNows || someRegs;
    }

    protected boolean anyRegulars() {
        return anyRemaining( regulars );
    }

    protected boolean anyNowables() {
        return anyRemaining( nowables );
    }

    protected int getGraceSeconds() {
        return graceSeconds;
    }

    static <T extends GracefulShutdownable> void append( List<T> appendTo, T toAdd ) {
        synchronized ( appendTo ) {
            appendTo.add( toAdd );
        }
    }

    static void clear( List<? extends GracefulShutdownable> toClear ) {
        synchronized ( toClear ) {
            toClear.clear();
        }
    }

    static boolean anyRemaining( List<? extends GracefulShutdownable> toCheck ) {
        synchronized ( toCheck ) {
            return !toCheck.isEmpty();
        }
    }

    // The following methods all process the lists backwards.
    // This facilitates less to shift on removal!

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    static boolean purgeDone( List<? extends GracefulShutdownable> toCheck ) {
        synchronized ( toCheck ) {
            if ( !toCheck.isEmpty() ) {
                for ( int i = toCheck.size() - 1; 0 <= i; i-- ) {
                    if ( toCheck.get( i ).isShutdown() ) {
                        toCheck.remove( i );
                    }
                }
            }
            return toCheck.isEmpty();
        }
    }

    static void gracefully( List<? extends GracefulShutdownable> gracefuls, List<Exception> problems ) {
        process( problems, gracefuls, GracefulShutdownable::shutdownGracefully );
    }

    static List<Exception> now( List<ShutdownNowable> nowables, List<Exception> problems, ShutdownNowable lastNowable ) {
        process( problems, nowables, ShutdownNowable::shutdownNow );
        if ( lastNowable != null ) {
            try {
                lastNowable.shutdownNow();
            }
            catch ( Exception e ) {
                problems.add( e );
            }
        }
        return problems;
    }

    static <T extends GracefulShutdownable> void process( @NotNull List<Exception> problems, List<T> toProcess, ExceptionalConsumer<T> consumer ) {
        synchronized ( toProcess ) {
            for ( int at = toProcess.size(); 0 <= --at; ) { // backwards!
                T instance = toProcess.get( at );
                try {
                    consumer.accept( instance );
                }
                catch ( Exception e ) {
                    problems.add( e );
                }
            }
        }
    }
}
