package com.hp.caf.worker.datastore.fs;


import com.hp.caf.api.ConfigurationException;
import com.hp.caf.api.ConfigurationSource;
import com.hp.caf.api.worker.DataStore;
import com.hp.caf.api.worker.DataStoreException;
import com.hp.caf.api.worker.DataStoreProvider;


public class FileSystemDataStoreProvider implements DataStoreProvider
{
    @Override
    public final DataStore getDataStore(final ConfigurationSource configurationSource)
            throws DataStoreException
    {
        try {
            return new FileSystemDataStore(configurationSource.getConfiguration(FileSystemDataStoreConfiguration.class));
        } catch (ConfigurationException e) {
            throw new DataStoreException("Cannot create data store", e);
        }
    }
}
