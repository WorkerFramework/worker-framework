/*
 * Copyright 2018-2017 EntIT Software LLC, a Micro Focus company.
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
package com.hpe.caf.worker.testing;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.TrackingInfo;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

/**
 * The {@code TaskMessageFactory} class creates messages for publishing to the worker queue.
 */
public class TaskMessageFactory
{
    private final java.lang.String CONTEXT_KEY = "context";
    private final byte[] CONTEXT_DATA = "testData".getBytes(StandardCharsets.UTF_8);
    private final Codec codec;
    private final String workerName;
    private final String workerInputQueueName;
    private final int apiVersion;

    /**
     * Instantiates a new Task message factory.
     *
     * @param codec the codec
     * @param workerName the worker name
     * @param apiVersion the api version
     */
    public TaskMessageFactory(final Codec codec, final String workerName, final String workerInputQueueName, final int apiVersion)
    {
        this.codec = codec;
        this.workerName = workerName;
        this.workerInputQueueName = workerInputQueueName;
        this.apiVersion = apiVersion;
    }

    /**
     * Create task message.
     *
     * @param workerTask the worker task
     * @param taskId the task id
     * @return the task message
     * @throws CodecException the codec exception
     */
    public TaskMessage create(final Object workerTask, TrackingInfo tracking, final String taskId) throws CodecException
    {
        Map<java.lang.String, byte[]> context = Collections.singletonMap(CONTEXT_KEY, CONTEXT_DATA);

        TaskMessage msg = new TaskMessage(taskId, workerName, apiVersion, codec.serialise(workerTask), TaskStatus.NEW_TASK, context, workerInputQueueName, tracking);
        return msg;
    }
}
