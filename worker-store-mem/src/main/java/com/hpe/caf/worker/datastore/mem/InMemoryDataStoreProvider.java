/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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
package com.hpe.caf.worker.datastore.mem;

import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.DataStoreProvider;
import com.hpe.caf.api.worker.ManagedDataStore;

public class InMemoryDataStoreProvider implements DataStoreProvider
{
    /**
     * Return a InMemoryDataStore. The configurationSource can be null as InMemoryDataStore currently does not take a configuration.
     *
     * @param configurationSource set this as null
     * @return the InMemoryDataStore
     */
    @Override
    public final ManagedDataStore getDataStore(final ConfigurationSource configurationSource)
    {
        return new InMemoryDataStore();
    }
}
