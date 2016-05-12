package com.hpe.caf.worker.datastore.cs;

import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.api.worker.DataStoreProvider;
import com.hpe.caf.api.worker.ManagedDataStore;
import com.hpe.caf.storage.sdk.StorageClient;

public class StorageServiceDataStoreProvider implements DataStoreProvider
{
    @Override
    public final ManagedDataStore getDataStore(final ConfigurationSource configurationSource)
            throws DataStoreException
    {
        try {
            StorageServiceDataStoreConfiguration storageServiceDataStoreConfiguration = configurationSource.getConfiguration(StorageServiceDataStoreConfiguration.class);
            KeycloakClient keycloakClient = storageServiceDataStoreConfiguration.getAuthenticationConfiguration() != null ? new KeycloakClient(storageServiceDataStoreConfiguration.getAuthenticationConfiguration()) : null;

            return new StorageServiceDataStore(configurationSource.getConfiguration(StorageServiceDataStoreConfiguration.class), keycloakClient);
        } catch (ConfigurationException e) {
            throw new DataStoreException("Cannot create object data store", e);
        }
    }
}
