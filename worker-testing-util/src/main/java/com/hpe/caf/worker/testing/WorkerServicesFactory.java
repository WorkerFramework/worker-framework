/*
 * Copyright 2015-2021 Micro Focus or one of its affiliates.
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
package com.hpe.caf.worker.testing;

import com.hpe.caf.api.*;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.api.worker.DataStoreProvider;
import com.hpe.caf.cipher.NullCipherProvider;
import com.hpe.caf.config.system.SystemBootstrapConfiguration;
import com.hpe.caf.naming.ServicePath;
import com.hpe.caf.util.ModuleLoader;
import com.hpe.caf.util.ModuleLoaderException;

/**
 * Created by ploch on 22/10/2015.
 */
public class WorkerServicesFactory
{
    private static BootstrapConfiguration bootstrapConfiguration = new SystemBootstrapConfiguration();

    private WorkerServicesFactory()
    {
    }

    public static WorkerServices create() throws ModuleLoaderException, CipherException, ConfigurationException, DataStoreException
    {

        Codec codec = ModuleLoader.getService(Codec.class);
        ConfigurationDecoderProvider decoderProvider = ModuleLoader.getService(ConfigurationDecoderProvider.class,
                                                                               CafConfigurationDecoderProvider.class);
        Decoder decoder = decoderProvider.getDecoder(bootstrapConfiguration, codec);
        Cipher cipher = ModuleLoader.getService(CipherProvider.class, NullCipherProvider.class).getCipher(bootstrapConfiguration);
        ServicePath path = bootstrapConfiguration.getServicePath();
        ConfigurationSource configurationSource = ModuleLoader.getService(ConfigurationSourceProvider.class).getConfigurationSource(bootstrapConfiguration, cipher, path, decoder);
        DataStore dataStore = ModuleLoader.getService(DataStoreProvider.class).getDataStore(configurationSource);

        return new WorkerServices(bootstrapConfiguration, codec, cipher, configurationSource, dataStore);
    }
}
