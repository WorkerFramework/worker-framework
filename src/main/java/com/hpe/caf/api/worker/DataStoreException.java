package com.hpe.caf.api.worker;


/**
 * Thrown by classes relevant to DataStore when it cannot handle a request.
 * @since 1.0
 */
public class DataStoreException extends Exception
{
    /** @since 8.0 **/
    public DataStoreException(final String message)
    {
        super(message);
    }


    public DataStoreException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}
