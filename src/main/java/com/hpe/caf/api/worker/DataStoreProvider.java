package com.hpe.caf.api.worker;


import com.hpe.caf.api.ConfigurationSource;


/**
 * Simple boilerplate to return a DataStore implementation.
 */
public interface DataStoreProvider
{
    DataStore getDataStore(final ConfigurationSource configurationSource)
        throws DataStoreException;
}
