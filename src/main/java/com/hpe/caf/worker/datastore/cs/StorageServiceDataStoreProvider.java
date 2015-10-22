package com.hpe.caf.worker.datastore.cs;

import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.api.worker.DataStoreProvider;
import com.hpe.caf.api.worker.ManagedDataStore;

public class StorageServiceDataStoreProvider implements DataStoreProvider
{
    @Override
    public final ManagedDataStore getDataStore(final ConfigurationSource configurationSource)
            throws DataStoreException
    {
        try {
            return new StorageServiceDataStore(configurationSource.getConfiguration(StorageServiceDataStoreConfiguration.class));
        } catch (ConfigurationException e) {
            throw new DataStoreException("Cannot create object data store", e);
        }
    }
}
