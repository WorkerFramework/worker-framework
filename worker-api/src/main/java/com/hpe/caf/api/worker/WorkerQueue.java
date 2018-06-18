/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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
 * A general representation of a queue for the purposes of a worker service.
 */
public interface WorkerQueue
{
    /**
     * Acknowledge the original received message but send out a new message to a target queue.
     *
     * @param acknowledgeId the internal queue message id of the message to acknowledge
     * @param taskMessage the message to publish
     * @param targetQueue the queue to put the message upon
     * @param headers the map of key/value paired headers to be stamped on the message
     * @param priority the message priority, greater value means higher priority
     * @throws QueueException if the message cannot be submitted
     */
    void publish(String acknowledgeId, byte[] taskMessage, String targetQueue, Map<String, Object> headers, int priority) throws QueueException;

    /**
     * Acknowledge the original received message but send out a new message to a target queue.
     *
     * @param acknowledgeId the internal queue message id of the message to acknowledge
     * @param taskMessage the message to publish
     * @param targetQueue the queue to put the message upon
     * @param headers the map of key/value paired headers to be stamped on the message
     * @throws QueueException if the message cannot be submitted
     */
    void publish(String acknowledgeId, byte[] taskMessage, String targetQueue, Map<String, Object> headers)
        throws QueueException;

    /**
     * Called from the asynchronous worker service to notify the queue that it is rejecting a task. It is up to the queue implementation
     * as to whether submit this task to retry or not.
     *
     * @param messageId the queue task id that has been rejected
     */
    void rejectTask(String messageId);

    /**
     * Called from the asynchronous worker service to notify the queue that it is discarding a task.
     *
     * @param messageId the queue task id that has been discarded
     */
    void discardTask(String messageId);

    /**
     * Called from the asynchronous worker service to notify the queue that it is acknowledging a task.
     *
     * @param messageId the queue task id that has been acknowledged
     */
    void acknowledgeTask(String messageId);

    /**
     * Return the name of the input queue.
     *
     * @return the name of the input queue
     */
    String getInputQueue();

}
