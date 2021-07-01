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
package com.hpe.caf.api.worker;

import com.hpe.caf.api.HealthReporter;

/**
 * Instantiates a new instance of a Worker given task-specific data. Most implementations of WorkerFactory should extend
 * DefaultWorkerFactory.
 */
public interface WorkerFactory extends HealthReporter
{
    /**
     * Instantiate a new worker for given task data
     *
     * @param classifier the classifier indicating the type of message it is
     * @param version the api version of the task's message
     * @param status the status of the task
     * @param data the raw serialised task data
     * @param context provides access to task specific context, may be null
     * @param tracking additional fields used in tracking task messages
     * @return a new Worker instance that will perform work upon the taskData
     * @throws TaskRejectedException if a Worker cannot be created to handle this task currently
     * @throws InvalidTaskException if it appears this task cannot possibly be handled by a Worker of this type
     */
    default Worker getWorker(String classifier, int version, TaskStatus status, Object data, byte[] context, TrackingInfo tracking)
        throws TaskRejectedException, InvalidTaskException
    {
        throw new UnsupportedOperationException();
    }

    default Worker getWorker(WorkerTaskData workerTask)
        throws TaskRejectedException, InvalidTaskException
    {
        return getWorker(workerTask.getClassifier(), workerTask.getVersion(), workerTask.getStatus(),
                         workerTask.getData(), workerTask.getContext(), workerTask.getTrackingInfo());
    }

    /**
     * @return the configuration used to instantiate workers.
     */
    default WorkerConfiguration getWorkerConfiguration()
    {
        return null;
    }

    /**
     * @return the queue to put responses to invalid tasks upon, may be the same as the Worker's result queue
     */
    String getInvalidTaskQueue();

    /**
     * @return the number of threads to be used by the framework to host this Worker backend
     */
    int getWorkerThreads();

    /**
     * Perform necessary cleanup of resources that the WorkerFactory was using. After this point, the factory can assume it will no longer
     * be called.
     */
    default void shutdown()
    {
    }
}
