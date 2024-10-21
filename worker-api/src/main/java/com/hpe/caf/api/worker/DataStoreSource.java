/*
 * Copyright 2015-2024 Open Text.
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

import com.github.cafapi.common.api.Codec;
import com.github.cafapi.common.api.CodecException;
import com.github.cafapi.common.api.QuietResource;
import com.github.cafapi.common.util.ref.DataSource;
import com.github.cafapi.common.util.ref.DataSourceException;
import com.github.cafapi.common.util.ref.SourceNotFoundException;

import java.io.InputStream;
import java.util.Objects;

/**
 * An implementation of a DataSource that uses a Worker DataStore and a CAF Codec.
 */
public class DataStoreSource extends DataSource
{
    private final DataStore store;
    private final Codec codec;

    /**
     * Create a new DataStoreSource.
     *
     * @param dataStore the store of remote data
     * @param codec the method to decode remote data into returned objects
     */
    public DataStoreSource(final DataStore dataStore, final Codec codec)
    {
        this.store = Objects.requireNonNull(dataStore);
        this.codec = Objects.requireNonNull(codec);
    }

    @Override
    public <T> T getObject(final String ref, final Class<T> clazz)
        throws DataSourceException
    {
        try (QuietResource<InputStream> qr = new QuietResource<>(getStream(ref))) {
            try {
                return codec.deserialise(qr.get(), clazz);
            } catch (CodecException e) {
                throw new DataSourceException("Could not deserialise stream", e);
            }
        }
    }

    @Override
    public InputStream getStream(final String ref)
        throws DataSourceException
    {
        try {
            return store.retrieve(ref);
        } catch (ReferenceNotFoundException e) {
            throw new SourceNotFoundException("Reference not found: " + ref, e);
        } catch (DataStoreException e) {
            throw new DataSourceException("Failed to get data stream", e);
        }
    }

    @Override
    public long getDataSize(final String ref)
        throws DataSourceException
    {
        try {
            return store.size(ref);
        } catch (ReferenceNotFoundException e) {
            throw new SourceNotFoundException("Reference not found: " + ref, e);
        } catch (DataStoreException e) {
            throw new DataSourceException("Failed to get data stream", e);
        }
    }
}
