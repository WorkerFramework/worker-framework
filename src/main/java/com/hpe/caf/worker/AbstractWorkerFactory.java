package com.hpe.caf.worker;


import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.DecodeMethod;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskRejectedException;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.Worker;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.api.worker.WorkerFactory;

import java.util.Objects;


/**
 * A WorkerFactory that uses a pre-defined configuration and Worker task class.
 * This is the recommended abstract base to create a WorkerFactory from.
 *
 * @param <C> the Worker Configuration type parameter
 * @param <T> the Worker Task type parameter
 * @since 9.0
 */
public abstract class AbstractWorkerFactory<C, T> implements WorkerFactory
{
    private final DataStore dataStore;
    private final C configuration;
    private final Codec codec;
    private final Class<T> taskClass;


    /**
     * Instantiates a new DefaultWorkerFactory.
     * @param configSource the worker configuration source
     * @param dataStore the external data store
     * @param codec the codec used in serialisation
     * @param configurationClass the worker configuration class
     * @param taskClass the worker task class
     * @throws WorkerException if the factory cannot be instantiated
     */
    public AbstractWorkerFactory(final ConfigurationSource configSource, final DataStore dataStore, final Codec codec,
                                 final Class<C> configurationClass, final Class<T> taskClass)
        throws WorkerException
    {
        this.codec = Objects.requireNonNull(codec);
        this.taskClass = Objects.requireNonNull(taskClass);
        this.dataStore = Objects.requireNonNull(dataStore);
        try {
            this.configuration = configSource.getConfiguration(configurationClass);
        } catch (ConfigurationException e) {
            throw new WorkerException("Failed to create worker factory", e);
        }
    }


    /**
     * {@inheritDoc}
     * Verify that the incoming task has the right type and is a version that can be handled.
     */
    @Override
    public final Worker getWorker(final String classifier, final int version, final TaskStatus status, final byte[] data, final byte[] context)
        throws TaskRejectedException, InvalidTaskException
    {
        // Reject tasks of the wrong type and tasks that require a newer version
        final String workerName = getWorkerName();
        if (!workerName.equals(classifier)) {
            return getMismatchedWorker(classifier, version, status, data, context);
        }

        final int workerApiVersion = getWorkerApiVersion();

        if (workerApiVersion < version) {
            throw new TaskRejectedException("Found task version " + version + ", which is newer than " + workerApiVersion);
        }

        try {
             return createWorker(codec.deserialise(data, taskClass, DecodeMethod.LENIENT));
        } catch (CodecException e) {
            throw new InvalidTaskException("Invalid input message", e);
        }
    }


    public Worker getMismatchedWorker(final String classifier, final int version, final TaskStatus status, final byte[] data, final byte[] context) throws InvalidTaskException {
        throw new InvalidTaskException("Task of type " + classifier + " found on queue for " + getWorkerName());
    }


    /**
     * @return the name of the worker to use when checking the task type
     */
    protected abstract String getWorkerName();


    /**
     * @return the maximum version of the worker message that is supported
     */
    protected abstract int getWorkerApiVersion();


    /**
     * Create a Worker instance.
     * @param task the deserialised Worker task
     * @return the worker instance
     * @throws TaskRejectedException if a Worker cannot be created to handle this task currently
     * @throws InvalidTaskException if it appears this task cannot possibly be handled by a Worker of this type
     */
    protected abstract Worker createWorker(final T task)
        throws TaskRejectedException, InvalidTaskException;


    /**
     * @return the data store supplied by the application
     */
    protected DataStore getDataStore()
    {
        return dataStore;
    }


    /**
     * @return the configuration associated with this WorkerFactory
     */
    protected C getConfiguration()
    {
        return configuration;
    }


    /**
     * @return the codec supplied by the application for de/serialisation purposes
     */
    protected Codec getCodec()
    {
        return codec;
    }
}
