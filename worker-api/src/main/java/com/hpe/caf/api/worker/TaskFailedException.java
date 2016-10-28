package com.hpe.caf.api.worker;


/**
 * An unchecked exception to throw if a Worker fails its task in an unrecoverable way.
 * @since 8.0
 */
public class TaskFailedException extends RuntimeException
{
    public TaskFailedException(final String message, final Throwable cause)
    {
        super(message, cause);
    }


    public TaskFailedException(final String message)
    {
        super(message);
    }
}
