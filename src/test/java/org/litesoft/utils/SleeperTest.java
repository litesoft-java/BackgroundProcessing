package org.litesoft.utils;

import org.junit.jupiter.api.Test;
import org.litesoft.SleeperBasedTestHelper;
import org.litesoft.pragmatics.Exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class SleeperTest extends SleeperBasedTestHelper {
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