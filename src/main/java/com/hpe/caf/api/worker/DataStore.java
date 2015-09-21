package com.hpe.caf.api.worker;


import com.hpe.caf.api.HealthReporter;

import java.io.InputStream;


/**
 * A representation of a generic data store, for reading and writing data
 * typically used by workers in the course of their computation.
 */
public abstract class DataStore implements HealthReporter
{
    /**
     * Get data by reference
     * @param reference the arbitrary string reference to a piece of data
     * @return the raw data referred to
     * @throws DataStoreException if the data store cannot service the request
     */
    public abstract InputStream getData(final String reference)
        throws DataStoreException;


    /**
     * Get the byte size of some data in the DataStore by reference
     * @param reference the arbitrary string reference to a piece of data
     * @return the size in bytes of the data being referred to
     * @throws DataStoreException if the data store cannot service the requeste
     */
    public abstract long getDataSize(final String reference)
        throws DataStoreException;


    /**
     * @return metrics for the data store
     */
    public abstract DataStoreMetricsReporter getMetrics();


    /**
     * Store data by reference
     * @param reference the arbitrary string reference to store the data by
     * @param data the raw data to store
     * @return reference to the stored data, which can be used to retrieve
     * @throws DataStoreException if the data store cannot service the request
     */
    public abstract String putData(final String reference, final InputStream data)
        throws DataStoreException;


    /**
     * Perform necessary shut down operations.
     */
    public abstract void shutdown();
}
