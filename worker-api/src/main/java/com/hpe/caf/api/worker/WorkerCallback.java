/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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
     * @param queueMsgId a queue-specific reference for the incoming message that generated the response
     * @param responseMessage the message to put on the queue (target specified by the {@code to} property)
     */
    void send(String queueMsgId, TaskMessage responseMessage);

    /**
     * Indicate a task was completed.
     *
     * @param queueMsgId a queue-specific reference for the incoming message that generated the response
     * @param queue the queue to hold the message
     * @param responseMessage the message to put on the queue
     */
    void complete(String queueMsgId, String queue, TaskMessage responseMessage);

    /**
     * Indicates the Worker wishes to abandon this task, but return it to the queue so that it can be retried by this or another Worker
     * instance.
     *
     * @param queueMsgId the id of the task's queue message to reject
     * @param e the Exception causing the task's queue message to be rejected
     */
    void abandon(String queueMsgId, Exception e);

    /**
     * Indicates the Worker wishes to forward this task to the specified queue without processing it.
     *
     * @param queueMsgId a queue-specific reference for the incoming message to be forwarded
     * @param queue the queue to hold the forwarded message
     * @param forwardedMessage the message to put on the queue
     * @param headers the map of key/value paired headers to be stamped on the message
     */
    void forward(String queueMsgId, String queue, TaskMessage forwardedMessage, Map<String, Object> headers);

    /**
     * Indicates the Worker wishes to discard this task without returning it to the queue for retry.
     *
     * @param queueMsgId the id of the task's queue message to discard
     */
    void discard(String queueMsgId);
}
