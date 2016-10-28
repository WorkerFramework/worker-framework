package com.hpe.caf.api.worker;


import java.io.InputStream;
import java.nio.file.Path;


/**
 * A representation of a generic data store, for reading and writing data
 * typically used by workers in the course of their computation.
 * @since 9.0
 */
public interface DataStore
{


    /**
     * Delete asset identified by reference
     * @param reference a complete reference to be interpreted by the DataStore implementation
     * @throws DataStoreException if data store cannot service the request
     * @since 11.0
     */
    void delete(String reference)
       throws DataStoreException;

    /**
     * Provide a stream to get data by reference
     * @param reference a complete reference to be interpreted by the DataStore implementation
     * @return the raw data referred to as a stream, which should be closed by the caller
     * @throws DataStoreException if the data store cannot service the request
     */
    InputStream retrieve(String reference)
        throws DataStoreException;


    /**
     * Get the byte size of some data in the DataStore by reference
     * @param reference a complete reference to be interpreted by the DataStore implementation
     * @return the size in bytes of the data being referred to
     * @throws DataStoreException if the data store cannot service the request
     * @since 10.0
     */
    long size(String reference)
        throws DataStoreException;


    /**
     * Store data from a stream, which should be closed by the caller. The data will be
     * stored relative to the partial reference supplied, and the absolute reference of
     * the final location will be returned.
     * @param dataStream the stream of data which will be read and put into the DataStore
     * @param partialReference the partial reference, which the data will be stored relative to
     * @return absolute reference to the stored data, which can be used to retrieve
     * @throws DataStoreException if the data store cannot service the request
     * @since 10.0
     */
    String store(InputStream dataStream, String partialReference)
        throws DataStoreException;


    /**
     * Store data from a byte array. The data will be stored relative to the partial
     * reference supplied, and the absolute reference of the final location will be returned.
     * @param data the raw byte data to store
     * @param partialReference the partial reference, which the data will be stored relative to
     * @return absolute reference to the stored data, which can be used to retrieve
     * @throws DataStoreException if the data store cannot service the request
     * @since 10.0
     */
    String store(byte[] data, String partialReference)
        throws DataStoreException;


    /**
     * Store data from a local file. The data will be stored relative to the partial
     * reference supplied, and the absolute reference of the final location will be returned.
     * @param dataPath path to a file on the local filesystem to store on the remote DataStore
     * @param partialReference the partial reference, which the data will be stored relative to
     * @return absolute reference to the stored data, which can be used to retrieve
     * @throws DataStoreException if the data store cannot service the request
     * @since 10.0
     */
    String store(Path dataPath, String partialReference)
        throws DataStoreException;
}
