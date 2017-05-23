/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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
package com.hpe.caf.api.worker;


import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.common.io.ByteStreams;
import com.hpe.caf.api.QuietResource;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.*;


/**
 * A representation of a generic data store, for reading and writing data
 * typically used by workers in the course of their computation.
 */
public interface DataStore
{


    /**
     * Delete asset identified by reference
     * @param reference a complete reference to be interpreted by the DataStore implementation
     * @throws DataStoreException if data store cannot service the request
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
     */
    String store(Path dataPath, String partialReference)
        throws DataStoreException;


    /**
     * Store data from a stream, which should be closed by the caller. The data will be
     * stored relative to the partial reference supplied, and the absolute reference of
     * the final location will be returned along with a SHA-1 hash of the data.
     * @param dataStream the stream of data which will be read and put into the DataStore
     * @param partialReference the partial reference, which the data will be stored relative to
     * @return absolute reference to the stored data, which can be used to retrieve, plus a SHA-1 hash of the stored data
     * @throws DataStoreException if the data store cannot service the request
     */
    default HashStoreResult hashStore(InputStream dataStream, String partialReference)
        throws DataStoreException
    {
        String hash;
        try (final QuietResource<PipedInputStream> streamToStore = new QuietResource<>(new PipedInputStream()))
        {
            final InputStream bufferedSource = new BufferedInputStream(dataStream);
            try {
                final HashingOutputStream hashStream =
                        new HashingOutputStream(Hashing.sha1(), new PipedOutputStream(streamToStore.get()));

                final Callable<Throwable> hashTask = () -> {
                    try {
                        try {
                            ByteStreams.copy(bufferedSource, hashStream);
                        } finally {
                            hashStream.close();
                        }
                        return null;
                    } catch (IOException e) {
                        return e;
                    }
                };

                ExecutorService executor = null;
                try {
                    executor = Executors.newSingleThreadExecutor();
                    final Future<Throwable> hashTaskResult = executor.submit(hashTask);
                    final String reference = store(streamToStore.get(), partialReference);
                    try {
                        final Throwable hashEx = hashTaskResult.get();
                        if (hashEx != null) {
                            throw new DataStoreException("Failed to hash data from a provided stream - hashing failed", hashEx);
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        throw new DataStoreException("Failed to hash data from a provided stream - hash task execution failed", e);
                    }
                    hash = Hex.encodeHexString(hashStream.hash().asBytes());
                    return new HashStoreResult(reference, hash);
                } finally {
                    if (executor != null) {
                        executor.shutdownNow();
                    }
                }
            } catch (IOException e) {
                throw new DataStoreException("Failed to hash data from a provided stream", e);
            }
        }
    }


    /**
     * Store data from a byte array. The data will be stored relative to the partial
     * reference supplied, and the absolute reference of the final location will be returned
     * along with a SHA-1 hash of the data.
     * @param data the raw byte data to store
     * @param partialReference the partial reference, which the data will be stored relative to
     * @return absolute reference to the stored data, which can be used to retrieve, plus a SHA-1 hash of the stored data
     * @throws DataStoreException if the data store cannot service the request
     */
    default HashStoreResult hashStore(byte[] data, String partialReference)
        throws DataStoreException
    {
        return new HashStoreResult(store(data, partialReference), DigestUtils.sha1Hex(data));
    }


    /**
     * Store data from a local file. The data will be stored relative to the partial
     * reference supplied, and the absolute reference of the final location will be returned
     * along with a SHA-1 hash of the data.
     * @param dataPath path to a file on the local filesystem to store on the remote DataStore
     * @param partialReference the partial reference, which the data will be stored relative to
     * @return absolute reference to the stored data, which can be used to retrieve, plus a SHA-1 hash of the stored data
     * @throws DataStoreException if the data store cannot service the request
     */
    default HashStoreResult hashStore(Path dataPath, String partialReference)
        throws DataStoreException
    {
        String hash;
        try (final InputStream dataStream = Files.newInputStream(dataPath)) {
            hash = DigestUtils.sha1Hex(dataStream);
        } catch (IOException e) {
            throw new DataStoreException("Failed to hash data from a provided filepath", e);
        }
        return new HashStoreResult(store(dataPath, partialReference), hash);
    }
}
