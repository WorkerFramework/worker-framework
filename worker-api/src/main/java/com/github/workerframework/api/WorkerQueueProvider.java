/*
 * Copyright 2015-2025 Open Text.
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
package com.github.workerframework.api;

import com.github.cafapi.common.api.Codec;
import com.github.cafapi.common.api.ConfigurationSource;

/**
 * Boilerplate for retrieving a WorkerQueue implementation.
 */
public interface WorkerQueueProvider
{
    /**
     * Create a new WorkerQueue instance.
     *
     * @param configurationSource used for configuring the WorkerQueue
     * @param maxTasks            the maximum number of tasks the worker can perform at once
     * @param invalidQueue        the queue in which to place tasks that are invalid or cannot be processed
     * @param dataStore           the managed data store that the worker will use to store data that exceeds a threshold.
     * @param codec               the codec used for serialization deserialization of data.
     * @return a new WorkerQueue instance
     * @throws QueueException if a WorkerQueue could not be created
     */
    ManagedWorkerQueue getWorkerQueue(ConfigurationSource configurationSource, int maxTasks, String invalidQueue,
                                      ManagedDataStore dataStore, Codec codec)
        throws QueueException;
}
