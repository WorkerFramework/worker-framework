package com.hpe.caf.api.worker;


import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.QuietResource;
import com.hpe.caf.util.ref.DataSource;
import com.hpe.caf.util.ref.DataSourceException;

import java.io.InputStream;
import java.util.Objects;


/**
 * An implementation of a DataSource that uses a Worker DataStore and a CAF Codec.
 * @since 9.0
 */
public class DataStoreSource extends DataSource
{
    private final DataStore store;
    private final Codec codec;


    /**
     * Create a new DataStoreSource.
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
        try ( QuietResource<InputStream> qr = new QuietResource<>(getStream(ref))) {
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
            return store.getInputStream(ref);
        } catch (DataStoreException e) {
            throw new DataSourceException("Failed to get data stream", e);
        }
    }
}
