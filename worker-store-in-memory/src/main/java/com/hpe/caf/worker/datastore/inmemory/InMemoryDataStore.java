package com.hpe.caf.worker.datastore.inmemory;

import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.worker.*;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import com.google.common.io.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * ManagedDataStore implementation for Amazon S3.
 */
public class InMemoryDataStore implements ManagedDataStore
{
    Map<String, byte[]> dataMap = new HashMap<>();

    /**
     * The Storage Service "file type" for Worker assets.
     */
    private final AtomicInteger errors = new AtomicInteger(0);
    private final AtomicInteger numRetrieveRequests = new AtomicInteger(0);
    private final AtomicInteger numStoreRequests = new AtomicInteger(0);
    private final AtomicInteger numDeleteRequests = new AtomicInteger(0);
    private final DataStoreMetricsReporter metrics = new InMemoryMetricsReporter();

    /**
     * Remove the asset identified by the reference.
     * @param reference a complete reference to be interpreted by the DataStore implementation
     * @throws DataStoreException if data store cannot service the request.
     */
    @Override
    public void delete(String reference) throws DataStoreException
    {
        numDeleteRequests.incrementAndGet();
        dataMap.remove(reference);
    }

    /**
     * Provide a stream to get data with the key reference.
     * @param reference a complete reference to be interpreted by the DataStore implementation
     * @return
     * @throws DataStoreException
     */
    @Override
    public InputStream retrieve(String reference) throws DataStoreException
    {
        numRetrieveRequests.incrementAndGet();
        return new ByteArrayInputStream(dataMap.get(reference));
    }

    /**
     * Return the byte size of the data at the reference.
     * @param reference a complete reference to be interpreted by the DataStore implementation
     * @return the byte size
     * @throws DataStoreException
     */
    @Override
    public long size(String reference) throws DataStoreException
    {
        return dataMap.get(reference).length;
    }

    /**
     * Store data from a stream, in the key given by the partial reference.
     * @param dataStream the stream of data which will be read and stored in the in memory data store.
     * @param partialReference the partial reference, which the data will be stored relative to.
     * @return absolute reference to the stored data which can be used to retrieve.
     * @throws DataStoreException if the data store cannot service the request.
     */
    @Override
    public String store(InputStream dataStream, String partialReference) throws DataStoreException
    {
        try {
            return store(IOUtils.toByteArray(dataStream), partialReference);
        } catch (IOException e) {
            errors.incrementAndGet();
            throw new DataStoreException("Could not store input stream.", e);
        }
    }

    /**
     * Store data from a byte array, in the key given by the partial reference.
     * @param data the raw byte data to store in the in-memory data store.
     * @param partialReference the partial reference, which the data will be stored relative to.
     * @return absolute reference to the stored data which can be used to retrieve.
     * @throws DataStoreException if the data store cannot service the request.
     */
    @Override
    public String store(byte[] data, String partialReference) throws DataStoreException
    {
        numStoreRequests.incrementAndGet();
        String absoluteReference = UUID.randomUUID().toString() + partialReference;
        dataMap.put(absoluteReference, data);
        return absoluteReference;
    }

    /**
     * Store data from a local file, in the key given by the partial reference.
     * @param dataPath path to a file on the local filesystem to store in the in-memory data store.
     * @param partialReference the partial reference, which the data will be stored relative to.
     * @return absolute reference to the stored data which can be used to retrieve.
     * @throws DataStoreException if the data store cannot service the request.
     */
    @Override
    public String store(Path dataPath, String partialReference) throws DataStoreException
    {
        try {
            return store(Files.asByteSource(dataPath.toFile()), partialReference);
        } catch (IOException e) {
            errors.incrementAndGet();
            throw new DataStoreException("Could not store file path.", e);
        }
    }

    @Override
    public DataStoreMetricsReporter getMetrics()
    {
        return metrics;
    }

    @Override
    public void shutdown()
    {
        // nothing to do
    }

    @Override
    public HealthResult healthCheck()
    {
        return HealthResult.RESULT_HEALTHY;
    }

    private class InMemoryMetricsReporter implements DataStoreMetricsReporter
    {

        @Override
        public int getDeleteRequests()
        {
            return numDeleteRequests.get();
        }

        @Override
        public int getStoreRequests()
        {
            return numStoreRequests.get();
        }

        @Override
        public int getRetrieveRequests()
        {
            return numRetrieveRequests.get();
        }

        @Override
        public int getErrors()
        {
            return errors.get();
        }
    }
}