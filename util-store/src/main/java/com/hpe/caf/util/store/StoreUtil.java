/*
 * Copyright 2015-2023 Open Text.
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
package com.hpe.caf.util.store;

import com.google.common.hash.Hashing;
import com.google.common.hash.HashingInputStream;
import com.hpe.caf.api.QuietResource;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreException;
import org.apache.commons.codec.binary.Hex;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Utility methods for use when storing data assets in DataStores.
 */
public final class StoreUtil
{
    private StoreUtil()
    {
    }

    /**
     * Store data from a stream, which should be closed by the caller. The data will be stored in the supplied DataStore relative to the
     * partial reference supplied and the absolute reference of the final location will be returned along with a SHA-1 hash of the data.
     *
     * @param dataStore the DataStore into which the data will be stored
     * @param dataStream the stream of data which will be read and put into the DataStore
     * @param partialReference the partial reference, which the data will be stored relative to
     * @return absolute reference to the stored data, which can be used to retrieve, plus a SHA-1 hash of the stored data
     * @throws DataStoreException if the data store cannot service the request
     */
    public static HashStoreResult hashStore(final DataStore dataStore, final InputStream dataStream, final String partialReference)
        throws DataStoreException
    {
        final HashingInputStream hashStream = new HashingInputStream(Hashing.sha1(), new BufferedInputStream(dataStream));
        final String reference = dataStore.store(hashStream, partialReference);
        final String hash = Hex.encodeHexString(hashStream.hash().asBytes());
        return new HashStoreResult(reference, hash);
    }

    /**
     * Store data from a byte array. The data will be stored in the supplied DataStore relative to the partial reference supplied, and the
     * absolute reference of the final location will be returned along with a SHA-1 hash of the data.
     *
     * @param dataStore the DataStore into which the data will be stored
     * @param data the raw byte data to store
     * @param partialReference the partial reference, which the data will be stored relative to
     * @return absolute reference to the stored data, which can be used to retrieve, plus a SHA-1 hash of the stored data
     * @throws DataStoreException if the data store cannot service the request
     */
    public static HashStoreResult hashStore(final DataStore dataStore, final byte[] data, final String partialReference)
        throws DataStoreException
    {
        try (final QuietResource<InputStream> dataStream = new QuietResource<>(new ByteArrayInputStream(data))) {
            return hashStore(dataStore, dataStream.get(), partialReference);
        }
    }

    /**
     * Store data from a local file. The data will be stored in the supplied DataStore relative to the partial reference supplied, and the
     * absolute reference of the final location will be returned along with a SHA-1 hash of the data.
     *
     * @param dataStore the DataStore into which the data will be stored
     * @param dataPath path to a file on the local filesystem to store on the remote DataStore
     * @param partialReference the partial reference, which the data will be stored relative to
     * @return absolute reference to the stored data, which can be used to retrieve, plus a SHA-1 hash of the stored data
     * @throws DataStoreException if the data store cannot service the request
     */
    public static HashStoreResult hashStore(final DataStore dataStore, final Path dataPath, final String partialReference)
        throws DataStoreException
    {
        try (final InputStream dataStream = Files.newInputStream(dataPath)) {
            return hashStore(dataStore, dataStream, partialReference);
        } catch (IOException e) {
            throw new DataStoreException("Failed to hash and store data from a provided filepath", e);
        }
    }

}
