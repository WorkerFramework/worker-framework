package com.hpe.caf.api.worker;


import com.hpe.caf.api.HealthReporter;

import java.io.InputStream;
import java.io.OutputStream;


/**
 * A representation of a generic data store, for reading and writing data
 * typically used by workers in the course of their computation.
 */
public abstract class DataStore implements HealthReporter
{
    /**
     * Provide a stream to Get data by reference
     * @param reference the arbitrary string reference to a piece of data
     * @return the raw data referred to
     * @throws DataStoreException if the data store cannot service the request
     */
    public abstract InputStream getInputStream(final String reference)
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
     * Provide a stream to Store data by reference
     * @param reference the arbitrary string reference to store the data by
     * @return reference to the stored data, which can be used to retrieve
     * @throws DataStoreException if the data store cannot service the request
     */
    public abstract OutputStream getOutputStream(final String reference)
        throws DataStoreException;


    /**
     * Perform necessary shut down operations.
     */
    public abstract void shutdown();
}
