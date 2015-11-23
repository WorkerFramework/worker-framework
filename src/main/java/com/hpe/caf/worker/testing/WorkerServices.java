package com.hpe.caf.worker.testing;

import com.hpe.caf.api.BootstrapConfiguration;
import com.hpe.caf.api.Cipher;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.DataStore;

/**
 * Created by ploch on 22/10/2015.
 */
public class WorkerServices {

    private static WorkerServices defaultWorkerServices;

    public static WorkerServices getDefault() throws Exception {
        if (defaultWorkerServices == null) {
            defaultWorkerServices = WorkerServicesFactory.create();
        }
        return defaultWorkerServices;
    }

    private final BootstrapConfiguration bootstrapConfiguration;
    private final Codec codec;
    private final Cipher cipher;
    private final ConfigurationSource configurationSource;
    private final DataStore dataStore;

    public WorkerServices(final BootstrapConfiguration bootstrapConfiguration, final Codec codec, final Cipher cipher, final ConfigurationSource configurationSource, final DataStore dataStore) {
        this.bootstrapConfiguration = bootstrapConfiguration;
        this.codec = codec;

        this.cipher = cipher;
        this.configurationSource = configurationSource;
        this.dataStore = dataStore;
    }

    /**
     * Getter for property 'bootstrapConfiguration'.
     *
     * @return Value for property 'bootstrapConfiguration'.
     */
    public BootstrapConfiguration getBootstrapConfiguration() {
        return bootstrapConfiguration;
    }

    /**
     * Getter for property 'codec'.
     *
     * @return Value for property 'codec'.
     */
    public Codec getCodec() {
        return codec;
    }

    /**
     * Getter for property 'cipher'.
     *
     * @return Value for property 'cipher'.
     */
    public Cipher getCipher() {
        return cipher;
    }

    /**
     * Getter for property 'configurationSource'.
     *
     * @return Value for property 'configurationSource'.
     */
    public ConfigurationSource getConfigurationSource() {
        return configurationSource;
    }

    /**
     * Getter for property 'dataStore'.
     *
     * @return Value for property 'dataStore'.
     */
    public DataStore getDataStore() {
        return dataStore;
    }
}
