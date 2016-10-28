package com.hpe.caf.util.ref;


/**
 * Thrown when the source reference is not found.
 * @since 1.0
 */
public class SourceNotFoundException extends DataSourceException
{
    public SourceNotFoundException(final String message)
    {
        super(message);
    }


    public SourceNotFoundException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}
