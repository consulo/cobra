package org.cobraparser.async;

/**
 * Used by methods that need to return results asynchronously. Results are
 * received in the event dispatch thread. This is a generic class that takes a
 * type parameter <code>TResult</code>, the type of the expected result object.
 */
public interface AsyncResult<TResult> {
    /**
     * Registers a listener of asynchronous results.
     *
     * @param listener
     */
    void addResultListener(AsyncResultListener<TResult> listener);

    /**
     * Removes a listener
     *
     * @param listener
     */
    void removeResultListener(AsyncResultListener<TResult> listener);

    /**
     * Forces listeners to be notified of a result if there is one
     */
    void signal();
}
