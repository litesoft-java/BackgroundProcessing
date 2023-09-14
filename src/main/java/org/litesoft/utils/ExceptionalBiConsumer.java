package org.litesoft.utils;

/**
 * Simple extension of the Java 8+ <code>BiConsumer</code> that can throw an <code>Exception</code>.
 */
@SuppressWarnings("unused")
public interface ExceptionalBiConsumer<T, U> {
    void accept( T t, U u )
            throws Exception;
}
