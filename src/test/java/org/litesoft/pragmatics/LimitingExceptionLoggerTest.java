package org.litesoft.pragmatics;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class LimitingExceptionLoggerTest implements LongSupplier,
                                             ExceptionLogger {
    private static final int NO_ENTRY = -1;
    private static final long OUR_TIME = Instant.parse( "2011-01-16T12:00:00.000Z" ).toEpochMilli();

    private long now = OUR_TIME;

    @Override
    public long getAsLong() { // LongSupplier
        return now;
    }

    protected final List<Exception> calls = new ArrayList<>();

    @Override
    public void log( Exception e ) {
        calls.add( e );
    }

    LimitingExceptionLogger logger = new LimitingExceptionLogger( 2, this, this );
    LimitingExceptionLogger.Tracker tracker = logger.tracker;

    @Test
    void log() {
        long now0000 = now;
        add( AA, AC, AS );
        assertCalls( AA, AC, AS );
        assertTimeEntries( 1 );
        assertTimeEntry( now0000, 3 );
        assertExceptionEntries( AA, AC, AS );

        long now0500 = now += 500; // half second
        add( BA, BC, BS );
        assertCalls( BA, BC, BS );
        assertTimeEntries( 2 );
        assertTimeEntry( now0000, 3 );
        assertTimeEntry( now0500, 3 );
        assertExceptionEntries( AA, AC, AS, BA, BC, BS );

        long now1000 = now += 500; // half second
        add( CA, CC, CS );
        assertCalls( CA, CC, CS );
        assertTimeEntries( 3 );
        assertTimeEntry( now0000, 3 );
        assertTimeEntry( now0500, 3 );
        assertTimeEntry( now1000, 3 );
        assertExceptionEntries( AA, AC, AS, BA, BC, BS, CA, CC, CS );

        long now1500 = now += 500; // half second
        add( AA, AC );
        assertCalls();
        assertTimeEntries( 3 );
        assertTimeEntry( now0000, 3 );
        assertTimeEntry( now0500, 3 );
        assertTimeEntry( now1000, 3 );
        assertTimeEntry( now1500, NO_ENTRY );
        assertExceptionEntries( AA, AC, AS, BA, BC, BS, CA, CC, CS );

        long now2000 = now += 500; // half second
        add( AS, BA, BC );
        assertCalls( AS );
        assertTimeEntries( 3 );
        assertTimeEntry( now0000, NO_ENTRY );
        assertTimeEntry( now0500, 3 );
        assertTimeEntry( now1000, 3 );
        assertTimeEntry( now1500, NO_ENTRY );
        assertTimeEntry( now2000, 1 );
        assertExceptionEntries( BA, BC, BS, CA, CC, CS, AS );

        long now2500 = now += 500; // half second
        add( AS, BC, BS, CA );
        assertCalls( BC, BS );
        assertTimeEntries( 3 );
        assertTimeEntry( now0000, NO_ENTRY );
        assertTimeEntry( now0500, NO_ENTRY );
        assertTimeEntry( now1000, 3 );
        assertTimeEntry( now1500, NO_ENTRY );
        assertTimeEntry( now2000, 1 );
        assertTimeEntry( now2500, 2 );
        assertExceptionEntries( CA, CC, CS, AS, BC, BS );
    }

    private void assertExceptionEntries( Exception... exceptions ) {
        assertEquals( exceptions.length, tracker.timeByExceptionNameAndMessage.size(), () -> "time: " + (now - OUR_TIME) );
    }

    private void assertTimeEntry( long expectedTimeEntry, int count ) {
        List<?> exceptionProxies = tracker.exceptionNameAndMessageByTimes.get( expectedTimeEntry );
        int size = (exceptionProxies == null) ? NO_ENTRY : exceptionProxies.size();
        assertEquals( count, size, () -> "expectedEntry (" + (expectedTimeEntry - OUR_TIME) + ") time: " + (now - OUR_TIME) );
    }

    private void assertTimeEntries( int count ) {
        assertEquals( count, tracker.exceptionNameAndMessageByTimes.size(), () -> "time: " + (now - OUR_TIME) );
    }

    private void add( Exception... exceptions ) {
        for ( Exception e : exceptions ) {
            logger.log( e );
        }
    }

    private void assertCalls( Exception... exceptions ) {
        assertEquals( exceptions.length, calls.size(), () -> "time: " + (now - OUR_TIME) );
        for ( int i = 0; i < calls.size(); i++ ) {
            assertSame( exceptions[i], calls.get( i ), () -> "time: " + (now - OUR_TIME) );
        }
        calls.clear();
    }

    private static final Exception AA = new IllegalArgumentException( "A" );
    private static final Exception AC = new IllegalCallerException( "A" );
    private static final Exception AS = new IllegalStateException( "A" );
    private static final Exception BA = new IllegalArgumentException( "B" );
    private static final Exception BC = new IllegalCallerException( "B" );
    private static final Exception BS = new IllegalStateException( "B" );
    private static final Exception CA = new IllegalArgumentException( "C" );
    private static final Exception CC = new IllegalCallerException( "C" );
    private static final Exception CS = new IllegalStateException( "C" );
}