package com.hpe.caf.util.ref;


import java.io.InputStream;


/**
 * Interface for defining how to retrieve objects or streams from a reference.
 * @since 1.0
 */
public interface DataSource
{
    /**
     * Retrieve an object of the specified class identified by a specific reference.
     * @param ref the reference that points to an instance of an object of the specified class
     * @param clazz the class of the object instance the reference points to
     * @param <T> the type of the object instance
     * @return the object instance of the specified class identified by the specified reference
     * @throws DataSourceException if the object instance cannot be acquired
     */
    <T> T getObject(final String ref, final Class<T> clazz)
        throws DataSourceException;


    /**
     * Retrieve a stream of data identified by a specific reference.
     * @param ref the reference that points to a stream of data
     * @return a stream of data identified by the specified reference
     * @throws DataSourceException if the data stream cannot be acquired
     */
    InputStream getStream(final String ref)
        throws DataSourceException;
}
