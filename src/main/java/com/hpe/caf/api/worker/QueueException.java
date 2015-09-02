package com.hpe.caf.api.worker;


/**
 * Thrown by classes related to WorkerQueue when a request cannot be handled.
 */
public class QueueException extends Exception
{
    public QueueException(final String message, final Throwable cause)
    {
        super(message, cause);
    }


    public QueueException(final String message)
    {
        super(message);
    }
}
