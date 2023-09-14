package org.litesoft.utils;

/**
 * Simple extension of the Java 8+ <code>LongConsumer</code> that can throw an <code>Exception</code>.
 */
@SuppressWarnings("unused")
public interface ExceptionalLongConsumer {
    void accept( long value )
            throws Exception;
}
