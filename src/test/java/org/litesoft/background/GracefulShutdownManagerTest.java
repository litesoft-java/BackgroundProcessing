package org.litesoft.background;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.litesoft.SleeperBasedTestHelper;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("ConstantValue")
class GracefulShutdownManagerTest extends SleeperBasedTestHelper {

    @Test
    void maxGraceSeconds() {
        GracefulShutdownManager shutdownManager = GracefulShutdownManager.INSTANCE.resetForTests();

        int minGraceSeconds = GracefulShutdownManager.MINIMUM_GRACE_SECS_ACCEPTABLE;
        int defaultGraceSecs = GracefulShutdownManager.DEFAULT_GRACE_SECS;

        assertTrue( minGraceSeconds > 0 );
        assertTrue( minGraceSeconds <= defaultGraceSecs );

        assertEquals( defaultGraceSecs, shutdownManager.getGraceSeconds() );

        try {
            shutdownManager.maxGraceSeconds( minGraceSeconds - 1 ); // Ignored
            assertEquals( defaultGraceSecs, shutdownManager.getGraceSeconds() );

            if ( 2 <= (defaultGraceSecs - minGraceSeconds) ) {
                int midGraceSecs = (minGraceSeconds + defaultGraceSecs) / 2;
                shutdownManager.maxGraceSeconds( midGraceSecs );
                assertEquals( midGraceSecs, shutdownManager.getGraceSeconds() );
            }
            shutdownManager.maxGraceSeconds( Integer.MAX_VALUE );
            assertEquals( Integer.MAX_VALUE, shutdownManager.getGraceSeconds() );
            assertFalse( shutdownManager.anyRegulars() );
            assertFalse( shutdownManager.anyNowables() );
        }
        finally {
            shutdownManager.resetForTests();
        }
    }

    @Test
    void shutdownNow() {
        GracefulShutdownManager shutdownManager =
                new GracefulShutdownManager( null, null )
                        .resetForTests().maxGraceSeconds( 2 )
                        .add( new MockShutdownNowable( "MSN1" ) )
                        .add( new MockShutdownNowable( "MSN2" ) );
        List<Exception> zExceptions = shutdownManager.shutdownNow();
        assertEquals( "SN(MSN2|2011-01-16T12:00:00Z)SN(MSN1|2011-01-16T12:00:00Z)", calls.toString() );
        // Note re above calls:
        //  clock never checked,
        //  isShutDown is never called,
        //  JUST shutdownNow on each 'ShutdownNowable' in reverse order of registration!
        assertEquals( 0, zExceptions.size(), zExceptions::toString );
    }

    @Test
    void shutdownGracefully() {
        GracefulShutdownManager shutdownManager =
                new GracefulShutdownManager( this, this )
                        .resetForTests().maxGraceSeconds( 2 )
                        .add( new MockShutdownNowable( "MSN" ) )
                        .add( new MockGracefulShutdownable( "MGN0", 0 ) )
                        .add( new MockGracefulShutdownable( "MGN1", 1 ) )
                        .add( new MockGracefulShutdownable( "MGN2", 2 ) );
        List<Exception> zExceptions = shutdownManager.shutdownGracefully();
        assertEquals(
                "g" + // Time
                "SG(MSN|2011-01-16T12:00:00Z)" + // Graceful shutdown requested
                "SG(MGN2|2011-01-16T12:00:00Z)" +
                "SG(MGN1|2011-01-16T12:00:00Z)" +
                "SG(MGN0|2011-01-16T12:00:00Z)" +
                "ISD(MSN):false" + // Check each for shutdown
                "ISD(MGN2):false" +
                "ISD(MGN1):true" + // 2 down!
                "ISD(MGN0):true" +
                "gga2g" + // pausing
                "ISD(MSN):false" + // Check remaining for shutdown
                "ISD(MGN2):true" + // 1 down -- Only Non-Graceful still running
                "gga2g" + // pausing
                "ISD(MSN):false" + // Check remaining for shutdown
                "g" + // pausing
                "SN(MSN|2011-01-16T12:00:02Z)", // shutdown NOW!
                calls.toString() );

        assertEquals( 0, zExceptions.size(), zExceptions::toString );
    }

    class AbstractMockShutdowner {
        final String name;
        boolean shutdown = false;

        public AbstractMockShutdowner( String pName ) {
            name = pName;
            acceptAdd = 1000;
        }

        public boolean isShutdown() {
            calls.append( "ISD(" ).append( name ).append( "):" ).append( shutdown );
            return shutdown;
        }

        protected void callWithTime( String what ) {
            calls.append( what )
                    .append( "(" )
                    .append( name ).append( "|" ).append( Instant.ofEpochMilli( now ) )
                    .append( ")" );
        }
    }

    class MockShutdownNowable extends AbstractMockShutdowner implements ShutdownNowable {
        public MockShutdownNowable( String pName ) {
            super( pName );
        }

        @Override
        public void shutdownGracefully() {
            callWithTime( "SG" );
        }

        @Override
        public void shutdownNow() {
            callWithTime( "SN" );
            shutdown = true;
        }
    }

    class MockGracefulShutdownable extends AbstractMockShutdowner implements GracefulShutdownable {
        boolean gracefulCalled = false;
        int shutDownOnNthIsShutdownCallAfterGracefulCall;

        public MockGracefulShutdownable( String pName, int shutdownOnCalls ) {
            super( pName );
            shutDownOnNthIsShutdownCallAfterGracefulCall = shutdownOnCalls;
        }

        @Override
        public boolean isShutdown() {
            if ( gracefulCalled && !shutdown ) {
                shutdown = --shutDownOnNthIsShutdownCallAfterGracefulCall == 0;
            }
            return super.isShutdown();
        }

        @Override
        public void shutdownGracefully() {
            callWithTime( "SG" );
            if ( !gracefulCalled ) {
                gracefulCalled = true;
                shutdown = shutDownOnNthIsShutdownCallAfterGracefulCall == 0;
            }
        }
    }
}