package org.litesoft.background;

import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

import org.litesoft.utils.ExceptionalConsumer;
import org.litesoft.utils.ExceptionalLongConsumer;
import org.litesoft.utils.Sleeper;

@SuppressWarnings("unused")
public class GracefulShutdownManager {
    public static final GracefulShutdownManager INSTANCE = new GracefulShutdownManager( System::currentTimeMillis, Thread::sleep );

    private final List<GracefulShutdownable> regulars = new ArrayList<>();
    private final List<ShutdownNowable> nowables = new ArrayList<>();
    private final LongSupplier millisTimeSource;
    private final Sleeper sleeper;

    private int graceSeconds = 10;

    public GracefulShutdownManager maxGraceSeconds( int seconds ) {
        if ( 2 <= seconds ) {
            graceSeconds = seconds;
        }
        return this;
    }

    public GracefulShutdownManager add( GracefulShutdownable shutdownable ) {
        if ( shutdownable != null ) {
            if ( shutdownable instanceof ShutdownNowable ) {
                nowables.add( (ShutdownNowable)shutdownable );
            } else {
                regulars.add( shutdownable );
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
                return now( nowables, problems );
            }
            if ( remainingMillis > 2 ) {
                sleeper.forMillis( 2 );
            }
        }
        return problems; // everything is Done!
    }

    public List<Exception> shutdownNow() {
        return now( nowables, new ArrayList<>() );
    }

    protected GracefulShutdownManager( LongSupplier millisTimeSource, ExceptionalLongConsumer sleepMethod ) {
        this.millisTimeSource = millisTimeSource;
        sleeper = new Sleeper( millisTimeSource, sleepMethod );
    }

    protected GracefulShutdownManager resetForTests() {
        regulars.clear();
        nowables.clear();
        graceSeconds = 10;
        return this;
    }

    protected boolean anyRemainingNotDone() {
        boolean someNows = !purgeDone( nowables );
        boolean someRegs = !purgeDone( regulars );
        return someNows || someRegs;
    }

    // The following methods all process the lists backwards.
    // This facilitates less to shift on removal!

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    static boolean purgeDone( List<? extends GracefulShutdownable> toCheck ) {
        if ( !toCheck.isEmpty() ) {
            for ( int i = toCheck.size() - 1; 0 <= i; i-- ) {
                if ( toCheck.get( i ).isShutdown() ) {
                    toCheck.remove( i );
                }
            }
        }
        return toCheck.isEmpty();
    }

    static void gracefully( List<? extends GracefulShutdownable> gracefuls, List<Exception> problems ) {
        process( problems, gracefuls, GracefulShutdownable::shutdownGracefully );
    }

    static List<Exception> now( List<ShutdownNowable> nowables, List<Exception> problems ) {
        return process( problems, nowables, ShutdownNowable::shutdownNow );
    }

    static <T extends GracefulShutdownable> List<Exception> process( List<Exception> problems, List<T> toProcess, ExceptionalConsumer<T> consumer ) {
        for ( int at = toProcess.size(); 0 <= --at; ) { // backwards!
            T instance = toProcess.get( at );
            try {
                consumer.accept( instance );
            }
            catch ( Exception e ) {
                problems.add( e );
            }
        }
        return problems;
    }
}
