package com.hpe.caf.worker.example;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.api.worker.WorkerFactory;
import com.hpe.caf.api.worker.WorkerFactoryProvider;

/**
 * Example worker factory provider implementation.
 */
public class ExampleWorkerFactoryProvider implements WorkerFactoryProvider {

    /**
     * Get the worker factory implementation.
     * Called by the ModuleLoader. ExampleWorkerFactoryProvider must be registered by the service file in resources/META-INF/services.
     * @param configSource
     * @param dataStore
     * @param codec
     * @return ExampleWorkerFactory
     * @throws WorkerException
     */
    @Override
    public WorkerFactory getWorkerFactory(final ConfigurationSource configSource, final DataStore dataStore, final Codec codec) throws WorkerException {
        return new ExampleWorkerFactory(configSource, dataStore, codec);
    }
}
