package com.hpe.caf.api.worker;


import com.hpe.caf.api.Codec;
import com.hpe.caf.api.ConfigurationSource;


/**
 * The responsibility of a WorkerFactory is to provide a mechanism to generate new Worker
 * objects and specify how many simultaneous workers should be running.
 * @since 5.0
 */
public interface WorkerFactoryProvider
{
    /**
     * Generate a new worker given task data.
     * @param configSource the configuration source optionally used to configure the workers
     * @param dataStore a datastore which is optionally available to workers
     * @param codec the Codec that can be used to serialise/deserialise data
     * @return a new worker
     * @throws WorkerException if a new Worker cannot be generated
     */
    WorkerFactory getWorkerFactory(final ConfigurationSource configSource, final DataStore dataStore, final Codec codec)
            throws WorkerException;


    /**
     * @return the number of simultaneous workers that should be in operation on this instance
     */
    int getWorkerThreads();
}
