package org.litesoft.background;

import org.litesoft.annotations.NotNull;
import org.litesoft.utils.ExceptionalRunnable;

/**
 * PulsingRunnable is an <code>ExceptionalRunnable</code> that presumes
 * that the run duration is short and can be called repeatedly (pulsed calls to run).
 */
public interface PulsingRunnable extends ExceptionalRunnable {
    static PulsingRunnable from( ExceptionalRunnable pulsingRunnable ) {
        NotNull.AssertArgument.namedValue( "pulsingRunnable", pulsingRunnable );
        return (pulsingRunnable instanceof PulsingRunnable) ?
               (PulsingRunnable)pulsingRunnable :
               pulsingRunnable::run;
    }

    static PulsingRunnable from( Runnable pulsingRunnable ) {
        return from( ExceptionalRunnable.from(
                NotNull.AssertArgument.namedValue( "pulsingRunnable", pulsingRunnable ) ) );
    }
}
