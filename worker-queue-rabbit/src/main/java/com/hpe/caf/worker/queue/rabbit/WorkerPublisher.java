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
package com.hpe.caf.worker.queue.rabbit;

import java.util.Map;

/**
 * A publisher that publishes to a specific queue and acknowledges another message subsequent to publishing.
 */
public interface WorkerPublisher
{
    /**
     * Publish a new message to a specified queue and acknowledge a prior message by id.
     *
     * @param data the body of the message to publish
     * @param routingKey the routing key to publish the new message with
     * @param ackId the prior message id to acknowledge
     * @param headers key/value map of headers to add to the published message
     * @param priority message priority, greater value means higher priority
     */
    void handlePublish(byte[] data, String routingKey, long ackId, Map<String, Object> headers, int priority);
}
