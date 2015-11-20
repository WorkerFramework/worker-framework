package com.hpe.caf.worker.testing;

import com.hpe.caf.api.*;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.api.worker.DataStoreProvider;
import com.hpe.caf.cipher.NullCipherProvider;
import com.hpe.caf.codec.JsonCodec;
import com.hpe.caf.config.system.SystemBootstrapConfiguration;
import com.hpe.caf.naming.ServicePath;
import com.hpe.caf.util.ModuleLoader;
import com.hpe.caf.util.ModuleLoaderException;

/**
 * Created by ploch on 22/10/2015.
 */
public class WorkerServicesFactory {

    private static BootstrapConfiguration bootstrapConfiguration = new SystemBootstrapConfiguration();
    private static Codec codec = new JsonCodec();

    private WorkerServicesFactory(){}

    public static WorkerServices create() throws ModuleLoaderException, CipherException, ConfigurationException, DataStoreException {
        Cipher cipher = ModuleLoader.getService(CipherProvider.class, NullCipherProvider.class).getCipher(bootstrapConfiguration);
        ServicePath path = bootstrapConfiguration.getServicePath();
        ConfigurationSource configurationSource = ModuleLoader.getService(ConfigurationSourceProvider.class).getConfigurationSource(bootstrapConfiguration, cipher, path, codec);
        DataStore dataStore = ModuleLoader.getService(DataStoreProvider.class).getDataStore(configurationSource);

        return new WorkerServices(bootstrapConfiguration, codec, cipher, configurationSource, dataStore);
    }

}
