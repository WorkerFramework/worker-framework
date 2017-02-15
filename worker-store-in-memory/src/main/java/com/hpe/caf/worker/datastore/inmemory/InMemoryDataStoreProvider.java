package com.hpe.caf.worker.datastore.inmemory;

import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.DataStoreProvider;
import com.hpe.caf.api.worker.ManagedDataStore;

public class InMemoryDataStoreProvider implements DataStoreProvider
{
    /**
     * Get the InMemoryDataStore object, which requires no configuration.
     * @return the InMemoryDataStore
     */
    public final ManagedDataStore getDataStore()
    {
        return getDataStore(null);
    }

    @Override
    public final ManagedDataStore getDataStore(final ConfigurationSource configurationSource)
    {
        return new InMemoryDataStore();
    }
}