/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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
package com.hpe.caf.worker.datastore.s3;

import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.api.worker.DataStoreProvider;
import com.hpe.caf.api.worker.ManagedDataStore;

public class S3DataStoreProvider implements DataStoreProvider
{
    @Override
    public final ManagedDataStore getDataStore(final ConfigurationSource configurationSource)
        throws DataStoreException
    {
        try {
            S3DataStoreConfiguration storageServiceDataStoreConfiguration = configurationSource.getConfiguration(S3DataStoreConfiguration.class);
            return new S3DataStore(storageServiceDataStoreConfiguration);
        } catch (ConfigurationException e) {
            throw new DataStoreException("Cannot create object data store", e);
        }
    }
}
