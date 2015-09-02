package com.hpe.caf.worker.datastore.fs;


import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.api.worker.DataStoreProvider;


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
