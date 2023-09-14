package org.litesoft.utils;

/**
 * Simple extension of the Java 8+ <code>Consumer</code> that can throw an <code>Exception</code>.
 */
@SuppressWarnings("unused")
public interface ExceptionalConsumer<T> {
    void accept( T t )
            throws Exception;
}
