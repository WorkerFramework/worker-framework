package com.hpe.caf.api.worker;


import com.hpe.caf.api.HealthReporter;

import java.io.InputStream;


/**
 * A representation of a generic data store, for reading and writing data
 * typically used by workers in the course of their computation.
 * @since 4.0
 */
public abstract class DataStore implements HealthReporter
{
    /**
     * Provide a stream to get data by reference
     * @param reference the arbitrary string reference to a piece of data
     * @return the raw data referred to
     * @throws DataStoreException if the data store cannot service the request
     * @since 8.0
     */
    public abstract InputStream retrieve(final String reference)
        throws DataStoreException;


    /**
     * Get the byte size of some data in the DataStore by reference
     * @param reference the arbitrary string reference to a piece of data
     * @return the size in bytes of the data being referred to
     * @throws DataStoreException if the data store cannot service the request
     * @since 8.0
     */
    public abstract long getDataSize(final String reference)
        throws DataStoreException;


    /**
     * @return metrics for the data store
     */
    public abstract DataStoreMetricsReporter getMetrics();


    /**
     * Provide a stream to store data, returning the reference it is stored by
     * @param dataStream the stream of data which will be read and put into the DataStore
     * @return reference to the stored data, which can be used to retrieve
     * @throws DataStoreException if the data store cannot service the request
     * @since 9.0
     */
    public abstract String store(final InputStream dataStream)
        throws DataStoreException;


    /**
     * Perform necessary shut down operations.
     */
    public abstract void shutdown();
}
