package com.hpe.caf.api.worker;


/**
 * Indicates the reference passed to the DataStore did not point to any resolvable location.
 * @since 9.0
 */
public class ReferenceNotFoundException extends DataStoreException
{
    public ReferenceNotFoundException(final String message)
    {
        super(message);
    }


    public ReferenceNotFoundException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}
