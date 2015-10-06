package com.hpe.caf.api.worker;


import com.hpe.caf.api.*;

import java.util.Objects;


/**
 * Instantiates a new instance of a Worker given task-specific data.
 *
 * @param <C> the Worker Configuration type parameter
 * @param <T> the Worker Task type parameter
 * @since 4.0
 */
public abstract class WorkerFactory<C, T> implements HealthReporter
{
    private final DataStore dataStore;
    private final C configuration;
    private final Codec codec;
    private Class<T> taskClass;

    /**
     * Instantiates a new Worker factory.
     *
     * @param configSource       the worker configuration source
     * @param dataStore          the external data store
     * @param codec              the codec used in serialisation
     * @param configurationClass the worker configuration class
     * @param taskClass          the worker task class
     * @throws WorkerException the worker exception
     */
    public WorkerFactory(final ConfigurationSource configSource, final DataStore dataStore, final Codec codec, Class<C> configurationClass, Class<T> taskClass)
            throws WorkerException
    {
        Objects.requireNonNull(configSource);
        Objects.requireNonNull(dataStore);
        Objects.requireNonNull(codec);
        Objects.requireNonNull(configurationClass);
        Objects.requireNonNull(taskClass);

        this.taskClass = taskClass;
        this.dataStore = dataStore;
        try {
            this.configuration = configSource.getConfiguration(configurationClass);
        } catch (ConfigurationException e) {
            throw new WorkerException("Error in creating worker factory", e);
        }
        this.codec = codec;
    }

    /**
     * Instantiate a new worker for given task data
     *
     * @param classifier the classifier indicating the type of message it is
     * @param version    the api version of the task's message
     * @param status     the status of the task
     * @param data       the raw serialised task data
     * @param context    provides access to task specific context, may be null
     * @return a new Worker instance that will perform work upon the taskData
     * @throws TaskRejectedException if a Worker cannot be created to handle this task currently
     * @throws InvalidTaskException  if it appears this task cannot possibly be handled by a Worker of this type
     * @since 6.0
     */
    public Worker getWorker(String classifier, int version, TaskStatus status, byte[] data, byte[] context) throws TaskRejectedException, InvalidTaskException {
        try {
            T task = codec.deserialise(data, taskClass);
            return createWorker(task);
        } catch (CodecException e) {
            throw new InvalidTaskException("Invalid input message", e);
        }
    }

    /**
     * Create worker instance.
     *
     * @param task the deserialised Worker task
     * @return the worker instance
     */
    protected abstract Worker createWorker(T task) throws InvalidTaskException;

    /**
     * @return the queue to put responses to invalid tasks upon, may be the same as the Worker's result queue
     * @since 8.0
     */
    public abstract String getInvalidTaskQueue();

    /**
     * Gets data store.
     *
     * @return the data store
     */
    protected DataStore getDataStore() {
        return dataStore;
    }

    /**
     * Gets configuration.
     *
     * @return the configuration
     */
    protected C getConfiguration() {
        return configuration;
    }

    /**
     * Gets codec.
     *
     * @return Value for property 'codec'.
     */
    public Codec getCodec() {
        return codec;
    }
}
