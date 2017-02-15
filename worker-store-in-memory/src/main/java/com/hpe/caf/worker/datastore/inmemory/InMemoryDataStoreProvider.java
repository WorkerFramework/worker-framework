package com.hpe.caf.worker.datastore.inmemory;

import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.api.worker.DataStoreProvider;
import com.hpe.caf.api.worker.ManagedDataStore;

public class InMemoryDataStoreProvider implements DataStoreProvider
{
    @Override
    public final ManagedDataStore getDataStore(final ConfigurationSource configurationSource)
        throws DataStoreException
    {
        return new InMemoryDataStore();
    }
}