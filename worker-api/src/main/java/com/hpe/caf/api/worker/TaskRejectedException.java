package com.hpe.caf.api.worker;


/**
 * Indicates that a task cannot be accepted right now, but that it should be retried
 * at a later time.
 * @since 7.0
 */
public class TaskRejectedException extends WorkerException
{
    public TaskRejectedException(final String message)
    {
        super(message);
    }


    public TaskRejectedException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}
