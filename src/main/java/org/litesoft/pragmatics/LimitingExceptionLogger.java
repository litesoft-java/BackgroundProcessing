package org.litesoft.pragmatics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;

import org.litesoft.annotations.NotNull;
import org.litesoft.annotations.Positive;

public class LimitingExceptionLogger implements ExceptionLogger {
    static final int MAX_TIME_ENTRIES_TO_PURGE_PER_CHECK = 2; // > 1 means that over time memory will not grow...
    private final ExceptionLogger actual;
    final Tracker tracker;

    protected LimitingExceptionLogger( int suppressSecs, LongSupplier millisTimeSource, ExceptionLogger actual ) {
        if ( actual instanceof LimitingExceptionLogger ) {
            throw new IllegalArgumentException( "actual ExceptionLogger can't be a LimitingExceptionLogger" );
        }
        this.actual = NotNull.AssertArgument.namedValue( "actual ExceptionLogger", actual );
        tracker = new Tracker(
                NotNull.AssertArgument.namedValue( "millisTimeSource", millisTimeSource ),
                Positive.AssertArgument.namedValue( "suppressSecs", suppressSecs )
        );
    }

    @SuppressWarnings("unused")
    public LimitingExceptionLogger( int delaySecs, ExceptionLogger actual ) {
        this( delaySecs, System::currentTimeMillis, actual );
    }

    @Override
    public void log( Exception e ) {
        if ( (e != null) && tracker.shouldShow( e ) ) {
            actual.log( e );
        }
    }

    static class Tracker {
        private record ExceptionNameAndMessage(String name, String message) {
        }

        private final LongSupplier millisTimeSource;

        // used in two ways: expiring records and
        private final long suppressMillis; // indicating that the new exception should be shown

        public Tracker( LongSupplier millisTimeSource, int suppressSecs ) {
            this.millisTimeSource = millisTimeSource;
            suppressMillis = suppressSecs * 1000L;
        }

        public boolean shouldShow( @NotNull Exception e ) {
            long now = millisTimeSource.getAsLong();
            var exceptionProxy = new ExceptionNameAndMessage( e.getClass().getName(), e.getMessage() );
            synchronized ( this ) {
                purgeExpiredState( now );
                return checkStateRe( exceptionProxy, now );
            }
        }

        final Map<Long, List<ExceptionNameAndMessage>> exceptionNameAndMessageByTimes = new LinkedHashMap<>();
        final Map<ExceptionNameAndMessage, Long> timeByExceptionNameAndMessage = new HashMap<>();

        // Executed under Object synchronization! Map(s) are safe to mess with!
        void purgeExpiredState( long now ) {
            if ( !exceptionNameAndMessageByTimes.isEmpty() ) {
                Long[] times = exceptionNameAndMessageByTimes.keySet().toArray( new Long[0] );
                int timesToProcess = Math.min( times.length, MAX_TIME_ENTRIES_TO_PURGE_PER_CHECK );
                for ( int i = 0; i < timesToProcess; i++ ) {
                    long time = times[i];
                    long expireTime = time + suppressMillis;
                    if ( now < expireTime ) {
                        return;
                    }
                    purgeTimeEntries( time );
                }
            }
        }

        // Executed under Object synchronization! Map(s) are safe to mess with!
        void purgeTimeEntries( long time ) {
            List<ExceptionNameAndMessage> toCheck = exceptionNameAndMessageByTimes.remove( time );
            for ( ExceptionNameAndMessage exceptionProxy : toCheck ) {
                Long currentAssociatedTime = timeByExceptionNameAndMessage.get( exceptionProxy );
                if ( (currentAssociatedTime != null) && (time == currentAssociatedTime) ) {
                    timeByExceptionNameAndMessage.remove( exceptionProxy );
                }
            }
        }

        // Executed under Object synchronization! Map(s) are safe to mess with!
        boolean checkStateRe( ExceptionNameAndMessage exceptionProxy, long now ) {
            Long currentAssociatedTime = timeByExceptionNameAndMessage.get( exceptionProxy );
            if ( currentAssociatedTime == null ) {
                return stateAdd( exceptionProxy, now );
            }
            if ( (currentAssociatedTime + suppressMillis) <= now ) {
                purgeTimeEntries( currentAssociatedTime );
                return stateAdd( exceptionProxy, now );
            }
            return false;
        }

        // Executed under Object synchronization! Map(s) are safe to mess with!
        boolean stateAdd( ExceptionNameAndMessage exceptionProxy, long now ) {
            timeByExceptionNameAndMessage.put( exceptionProxy, now );
            List<ExceptionNameAndMessage> addTo = exceptionNameAndMessageByTimes.computeIfAbsent( now, ( key ) -> new ArrayList<>() );
            addTo.add( exceptionProxy );
            return true;
        }
    }
}
