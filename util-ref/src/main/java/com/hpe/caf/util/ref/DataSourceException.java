package com.hpe.caf.util.ref;


/**
 * General exception for DataSource acquisition failures.
 * @since 1.0
 */
public class DataSourceException extends Exception
{
    public DataSourceException(final String message)
    {
        super(message);
    }


    public DataSourceException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}
