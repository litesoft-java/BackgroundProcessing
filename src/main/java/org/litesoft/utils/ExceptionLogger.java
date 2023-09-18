package org.litesoft.utils;

import java.io.PrintStream;

import org.litesoft.annotations.NotNull;

@SuppressWarnings("unused")
public interface ExceptionLogger {
    void log( Exception e );

    static ExceptionLogger with( PrintStream ps ) {
        NotNull.AssertError.namedValue( "PrintStream", ps );
        return ( e ) -> {
            if ( e != null ) {
                e.printStackTrace( System.out );
            }
        };
    }
}
