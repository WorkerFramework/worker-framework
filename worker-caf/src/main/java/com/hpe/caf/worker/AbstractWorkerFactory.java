/*
 * (c) Copyright 2015-2016 Hewlett Packard Enterprise Development LP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hpe.caf.worker;


import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.DecodeMethod;
import com.hpe.caf.api.worker.*;

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
    public final Worker getWorker(final String classifier, final int version, final TaskStatus status, final byte[] data, final byte[] context, TrackingInfo tracking)
        throws TaskRejectedException, InvalidTaskException
    {
        return createWorker(verifyWorkerTask(classifier, version, data), tracking);
    }


    /**
     * @return the configuration used to instantiate workers.
     */
    @Override
    public WorkerConfiguration getWorkerConfiguration() {
        if (configuration instanceof WorkerConfiguration) {
            return (WorkerConfiguration)configuration;
        }
        return null;
    }


    /**
     * Verify that the specified worker task has the right type and is a version
     * that can be handled.
     */
    protected final T verifyWorkerTask(final WorkerTask workerTask)
        throws TaskRejectedException, InvalidTaskException
    {
        return verifyWorkerTask(workerTask.getClassifier(),
                                workerTask.getVersion(),
                                workerTask.getData());
    }


    /**
     * Verify that the specified worker task has the right type and is a version
     * that can be handled.
     * 
     * <p>Note that whereas verifyWorkerTask() will throw an exception when
     * there is an issue, this method will instead use the setResponse() method
     * on the WorkerTask and then return null.</p>
     */
    protected final T verifyWorkerTaskAndSetResponse(final WorkerTask workerTask)
    {
        try {
            return verifyWorkerTask(workerTask);
        } catch (TaskRejectedException ex) {
            workerTask.setResponse(ex);
            return null;
        } catch (InvalidTaskException ex) {
            workerTask.setResponse(ex);
            return null;
        }
    }


    /**
     * Verify that the specified worker task has the right type and is a version
     * that can be handled.
     */
    private T verifyWorkerTask
    (
        final String classifier,
        final int version,
        final byte[] data
    )
        throws TaskRejectedException, InvalidTaskException
    {
        // Reject tasks of the wrong type and tasks that require a newer version
        final String workerName = getWorkerName();
        if (!workerName.equals(classifier)) {
            throw new InvalidTaskException("Task of type " + classifier + " found on queue for " + getWorkerName());
        }

        final int workerApiVersion = getWorkerApiVersion();

        if (workerApiVersion < version) {
            throw new TaskRejectedException("Found task version " + version + ", which is newer than " + workerApiVersion);
        }

        if (data == null) {
            throw new InvalidTaskException("Invalid input message: task not specified");
        }

        try {
            return codec.deserialise(data, taskClass, DecodeMethod.LENIENT);
        } catch (CodecException e) {
            throw new InvalidTaskException("Invalid input message", e);
        }
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
     * Create a Worker instance with access to the tracking info associated with the task.
     * This method should be overridden by any Worker Factory creating Worker instances that need access to tracking info.
     * @param task the deserialised Worker task
     * @param tracking additional fields used in tracking task messages
     * @return the worker instance
     * @throws TaskRejectedException if a Worker cannot be created to handle this task currently
     * @throws InvalidTaskException if it appears this task cannot possibly be handled by a Worker of this type
     */
    protected Worker createWorker(final T task, TrackingInfo tracking) throws TaskRejectedException, InvalidTaskException {
        return createWorker(task);
    }


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
