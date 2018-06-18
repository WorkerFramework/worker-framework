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
package com.hpe.caf.worker.testing.data;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreSource;
import com.hpe.caf.util.ref.DataSourceException;
import com.hpe.caf.util.ref.ReferencedData;

import java.io.InputStream;

/**
 * Created by ploch on 08/12/2015.
 */
public class ContentDataHelper
{
    public static InputStream retrieveReferencedData(DataStore dataStore, Codec codec, ReferencedData referencedData) throws DataSourceException
    {
        DataStoreSource source = new DataStoreSource(dataStore, codec);

        return referencedData.acquire(source);
    }
}
