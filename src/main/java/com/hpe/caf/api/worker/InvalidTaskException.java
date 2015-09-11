package com.hpe.caf.api.worker;


/**
 * Indicates this task is fundamentally flawed in some manner, and is likely
 * an error situation that would not be resolved through retrying.
 */
public class InvalidTaskException extends WorkerException
{
    public InvalidTaskException(final String message)
    {
        super(message);
    }


    public InvalidTaskException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}
