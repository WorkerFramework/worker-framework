/*
 * Copyright 2015-2024 Open Text.
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
package com.opentext.caf.worker.datastore.http;

import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.opentext.caf.api.worker.DataStoreException;
import com.opentext.caf.api.worker.DataStoreProvider;
import com.opentext.caf.api.worker.ManagedDataStore;

public class HttpDataStoreProvider implements DataStoreProvider
{
    @Override
    public final ManagedDataStore getDataStore(final ConfigurationSource configurationSource)
        throws DataStoreException
    {
        try {
            return new HttpDataStore(configurationSource.getConfiguration(HttpDataStoreConfiguration.class));
        } catch (ConfigurationException e) {
            throw new DataStoreException("Cannot create data store", e);
        }
    }
}
