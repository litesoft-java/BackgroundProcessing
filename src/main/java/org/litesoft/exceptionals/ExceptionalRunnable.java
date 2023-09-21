package org.litesoft.exceptionals;

import org.litesoft.annotations.NotNull;

/**
 * Similar to a <code>Callable<Void></code> but simpler to use.
 */
@SuppressWarnings("unused")
public interface ExceptionalRunnable {
    void run()
            throws Exception;

    static ExceptionalRunnable from( Runnable runnable ) {
        NotNull.AssertArgument.namedValue( "runnable", runnable );
        return runnable::run;
    }
}
