package org.litesoft.utils;

import java.time.Instant;
import java.util.function.LongSupplier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class SleeperTest implements LongSupplier,
                             ExceptionalLongConsumer {
    private static final long OUR_TIME = Instant.parse( "2011-01-16T12:00:00.000Z" ).toEpochMilli();

    private final StringBuilder calls = new StringBuilder();

    private long now = OUR_TIME;
    private Exception toThrow;
    private int acceptAdd = 0;

    @Override
    public long getAsLong() {
        calls.append( 'g' );
        return now;
    }

    @Override
    public void accept( long value )
            throws Exception {
        calls.append( 'a' ).append( value );
        now += acceptAdd;
        Exception throwing = toThrow;
        toThrow = null;
        if ( throwing != null ) {
            calls.append( '(' ).append( throwing.getClass().getSimpleName() ).append( ')' );
            throw throwing;
        }
    }

    @Test
    void forMillis1() {
        Sleeper sleeper = new Sleeper( this, this );
        acceptAdd = 2;
        sleeper.forMillis( 6 );
        assertEquals( "ga6ga4ga2g", calls.toString() );
    }

    @Test
    void forMillis2() {
        Sleeper sleeper = new Sleeper( this, this );
        acceptAdd = 2;
        toThrow = new IllegalStateException();
        try {
            sleeper.forMillis( 6 );
            fail( "expected IllegalStateException" );
        }
        catch ( IllegalStateException expected ) {
            Exceptions.swallowExpected( expected );
        }
        assertEquals( "ga6(IllegalStateException)", calls.toString() );
    }

    @Test
    void forMillis4() {
        Sleeper sleeper = new Sleeper( this, this );
        acceptAdd = 2;
        toThrow = new InterruptedException();
        sleeper.forMillis( 6 );
        assertEquals( "ga6(InterruptedException)ga4ga2g", calls.toString() );
    }
}