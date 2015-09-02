package com.hpe.caf.api.worker;


/**
 * Thrown by classes related to Worker when a request cannot be handled.
 */
public class WorkerException extends Exception
{
    public WorkerException(final String message, final Throwable cause)
    {
        super(message, cause);
    }

    public WorkerException(final  String message) {
        super(message);
    }
}
