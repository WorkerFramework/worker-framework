/*
 * Copyright 2015-2024 Open Text.
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
package com.github.workerframework.worker.api;

import java.util.Map;

/**
 * A callback interface used to announce the arrival of a new task for a worker to process or signal that the core should cancel its
 * tasks. Generally called from a WorkerQueue implementation.
 */
public interface TaskCallback
{
    /**
     * Announce to the worker core that a new task has been picked off the queue for processing.
     *
     * @param taskInformation contains an arbitrary task reference
     * @param taskData the task data that is specific to the workers hosted
     * @param headers the map of key/value paired headers on the message
     * @throws TaskRejectedException if the worker framework rejected execution of the task at this time
     * @throws InvalidTaskException if the worker framework indicates this task is invalid and cannot possibly be executed
     */
    void registerNewTask(TaskInformation taskInformation, byte[] taskData, Map<String, Object> headers)
        throws TaskRejectedException, InvalidTaskException;

    /**
     * Signal that any tasks queued or in operation should be aborted. This usually means there was a problem with the queue and any
     * accepted messages should be considered void.
     */
    void abortTasks();
}
