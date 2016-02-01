package com.hpe.caf.worker.example;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.api.worker.WorkerFactory;
import com.hpe.caf.api.worker.WorkerFactoryProvider;

/**
 * Created by smitcona on 22/01/2016.
 */
public class ExampleWorkerFactoryProvider implements WorkerFactoryProvider {

    /**
     * Get the worker factory implementation.
     * Called by ModuleLoader providing the ExampleWorkerFactoryProvider is registered by the service file in META-INF > services
     * @param configSource
     * @param dataStore
     * @param codec
     * @return
     * @throws WorkerException
     */
    @Override
    public WorkerFactory getWorkerFactory(final ConfigurationSource configSource, final DataStore dataStore, final Codec codec) throws WorkerException {
        return new ExampleWorkerFactory(configSource, dataStore, codec);
    }
}
