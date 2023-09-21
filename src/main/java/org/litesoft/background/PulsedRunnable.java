package org.litesoft.background;

import org.litesoft.annotations.NotNull;
import org.litesoft.exceptionals.ExceptionalRunnable;

/**
 * PulsingRunnable is an <code>ExceptionalRunnable</code> that presumes
 * that the run duration is short and can be called repeatedly (pulsed calls to run).
 */
public interface PulsedRunnable extends ExceptionalRunnable {
    static PulsedRunnable from( ExceptionalRunnable pulsedRunnable ) {
        NotNull.AssertArgument.namedValue( "pulsedRunnable", pulsedRunnable );
        return (pulsedRunnable instanceof PulsedRunnable) ?
               (PulsedRunnable)pulsedRunnable :
               pulsedRunnable::run;
    }

    static PulsedRunnable from( Runnable pulsedRunnable ) {
        return from( ExceptionalRunnable.from(
                NotNull.AssertArgument.namedValue( "pulsedRunnable", pulsedRunnable ) ) );
    }
}
