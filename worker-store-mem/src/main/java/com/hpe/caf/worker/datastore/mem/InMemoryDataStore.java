/*
 * Copyright 2015-2020 Micro Focus or one of its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hpe.caf.worker.datastore.mem;

import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.api.worker.DataStoreMetricsReporter;
import com.hpe.caf.api.worker.ManagedDataStore;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ManagedDataStore implementation for an In Memory Datastore which uses a HashMap.
 */
public class InMemoryDataStore implements ManagedDataStore
{

    public InMemoryDataStore()
    {
        // No need to do anything in constructor
    }

    /**
     * We will use a HashMap as the data storage structure.
     */
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
     *
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
     * Provide a stream to get data identified by the reference.
     *
     * @param reference a complete reference to be interpreted by the DataStore implementation
     * @return
     * @throws DataStoreException
     */
    @Override
    public InputStream retrieve(String reference) throws DataStoreException
    {
        numRetrieveRequests.incrementAndGet();
        if (!dataMap.containsKey(reference)) {
            errors.incrementAndGet();
            throw new DataStoreException("Failed to retrieve data, the asset does not exist.");
        }
        return new ByteArrayInputStream(dataMap.get(reference));
    }

    /**
     * Return the byte size of the data identified by the reference.
     *
     * @param reference a complete reference to be interpreted by the DataStore implementation
     * @return the byte size
     * @throws DataStoreException
     */
    @Override
    public long size(String reference) throws DataStoreException
    {
        if (!dataMap.containsKey(reference)) {
            throw new DataStoreException("Failed to retrieve data, the asset does not exist.");
        }
        return dataMap.get(reference).length;
    }

    /**
     * Store data from a stream in the key given by the partial reference.
     *
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
     * Store data from a byte array in the key given by the partial reference.
     *
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
     * Store data from a local file in the key given by the partial reference.
     *
     * @param dataPath path to a file on the local filesystem to store in the in-memory data store.
     * @param partialReference the partial reference, which the data will be stored relative to.
     * @return absolute reference to the stored data which can be used to retrieve.
     * @throws DataStoreException if the data store cannot service the request.
     */
    @Override
    public String store(Path dataPath, String partialReference) throws DataStoreException
    {
        try {
            return store(Files.readAllBytes(dataPath), partialReference);
        } catch (IOException e) {
            errors.incrementAndGet();
            throw new DataStoreException("Could not store file path.", e);
        }
    }

    /**
     * Get the Metrics for the in memory data store.
     *
     * @return metrics
     */
    @Override
    public DataStoreMetricsReporter getMetrics()
    {
        return metrics;
    }

    /**
     * Perform necessary shut down operations.
     */
    @Override
    public void shutdown()
    {
        // nothing to do
    }

    /**
     * Always return a RESULT_HEALTHY health check as the data store is in memory.
     *
     * @return RESULT_HEALTHY
     */
    @Override
    public HealthResult healthCheck()
    {
        return HealthResult.RESULT_HEALTHY;
    }

    /**
     * In memory data store metrics reporter.
     */
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
