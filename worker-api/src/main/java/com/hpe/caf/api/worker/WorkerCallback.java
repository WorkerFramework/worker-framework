/*
 * Copyright 2015-2020 Micro Focus or one of its affiliates.
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

import java.util.Map;

/**
 * The callback interface for a task to report it is complete or that it must be subject to some further action, e.g. forwarding.
 */
public interface WorkerCallback
{
    /**
     * Used to send responses prior to the final response (when complete should be used instead).
     *
     * @param taskInformation a queue-specific reference for the incoming message that generated the response
     * @param responseMessage the message to put on the queue (target specified by the {@code to} property)
     */
    void send(TaskInformation taskInformation, TaskMessage responseMessage);

    /**
     * Indicate a task was completed.
     *
     * @param taskInformation a queue-specific reference for the incoming message that generated the response
     * @param queue the queue to hold the message
     * @param responseMessage the message to put on the queue
     */
    void complete(TaskInformation taskInformation, String queue, TaskMessage responseMessage);

    /**
     * Indicates the Worker wishes to abandon this task, but return it to the queue so that it can be retried by this or another Worker
     * instance.
     *
     * @param taskInformation the id of the task's queue message to reject
     * @param e the Exception causing the task's queue message to be rejected
     */
    void abandon(TaskInformation taskInformation, Exception e);

    /**
     * Indicates the Worker wishes to forward this task to the specified queue without processing it.
     *
     * @param taskInformation a queue-specific reference for the incoming message to be forwarded
     * @param queue the queue to hold the forwarded message
     * @param forwardedMessage the message to put on the queue
     * @param headers the map of key/value paired headers to be stamped on the message
     */
    void forward(TaskInformation taskInformation, String queue, TaskMessage forwardedMessage, Map<String, Object> headers);

    /**
     * Indicates the Worker wishes to discard this task without returning it to the queue for retry.
     *
     * @param taskInformation the id of the task's queue message to discard
     */
    void discard(TaskInformation taskInformation);

    /**
     * Used to send a report update message.
     *
     * @param taskInformation a queue-specific reference for the incoming message
     * @param reportUpdateMessage the report update message to put on the queue
     */
    void reportUpdate(final TaskInformation taskInformation, final TaskMessage reportUpdateMessage);
}
