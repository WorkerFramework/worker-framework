package com.hpe.caf.api.worker;


import java.io.InputStream;


/**
 * A representation of a generic data store, for reading and writing data
 * typically used by workers in the course of their computation.
 * @since 9.0
 */
public interface DataStore
{
    /**
     * Provide a stream to get data by reference
     * @param reference the arbitrary string reference to a piece of data
     * @return the raw data referred to
     * @throws DataStoreException if the data store cannot service the request
     */
    InputStream retrieve(String reference)
        throws DataStoreException;


    /**
     * Get the byte size of some data in the DataStore by reference
     * @param reference the arbitrary string reference to a piece of data
     * @return the size in bytes of the data being referred to
     * @throws DataStoreException if the data store cannot service the request
     */
    long getDataSize(String reference)
        throws DataStoreException;


    /**
     * Provide a stream to store data, returning the reference it is stored by
     * @param dataStream the stream of data which will be read and put into the DataStore
     * @return reference to the stored data, which can be used to retrieve
     * @throws DataStoreException if the data store cannot service the request
     */
    String store(InputStream dataStream)
        throws DataStoreException;
}
