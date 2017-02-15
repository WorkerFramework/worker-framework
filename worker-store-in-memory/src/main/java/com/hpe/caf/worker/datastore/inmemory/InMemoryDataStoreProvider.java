package com.hpe.caf.worker.datastore.inmemory;

import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.DataStoreProvider;
import com.hpe.caf.api.worker.ManagedDataStore;

public class InMemoryDataStoreProvider implements DataStoreProvider
{
    /**
     * Return a InMemoryDataStore. The configurationSource can be null as InMemoryDataStore currently does not take a configuration.
     * @param configurationSource set this as null
     * @return the InMemoryDataStore
     */
    @Override
    public final ManagedDataStore getDataStore(final ConfigurationSource configurationSource)
    {
        return new InMemoryDataStore();
    }
}