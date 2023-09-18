package org.litesoft;

import java.time.Instant;
import java.util.function.LongSupplier;

import org.litesoft.utils.ExceptionalLongConsumer;

public abstract class SleeperBasedTestHelper implements LongSupplier,
                                                        ExceptionalLongConsumer {

    protected static final long OUR_TIME = Instant.parse( "2011-01-16T12:00:00.000Z" ).toEpochMilli();

    protected final StringBuilder calls = new StringBuilder();

    protected long now = OUR_TIME;
    protected Exception toThrow;
    protected int acceptAdd = 0;

    @Override
    public long getAsLong() { // LongSupplier
        calls.append( 'g' );
        return now;
    }

    @Override
    public void accept( long value ) // ExceptionalLongConsumer
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
}
