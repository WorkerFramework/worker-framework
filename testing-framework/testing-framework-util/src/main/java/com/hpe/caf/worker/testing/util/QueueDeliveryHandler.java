/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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
package com.hpe.caf.worker.testing.util;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.DecodeMethod;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.worker.testing.api.TaskMessageHandler;
import com.hpe.caf.worker.testing.api.TestContext;
import com.hpe.caf.worker.testing.api.TestFailedException;

import java.util.Map;
import java.util.Objects;

/**
 * Created by ploch on 06/03/2017.
 */
public class QueueDeliveryHandler implements TaskCallback {

    private final Codec codec;

    private final TaskMessageHandler messageHandler;
    private final ManagedWorkerQueue workerQueue;


    public QueueDeliveryHandler(final TaskMessageHandler messageHandler, final ManagedWorkerQueue workerQueue, final Codec codec) {
        this.codec = Objects.requireNonNull(codec);

        this.messageHandler = messageHandler;
        this.workerQueue = workerQueue;
    }


    /**
     * {@inheritDoc}
     * <p>
     * Use the factory to get a new worker to handle the task, wrap this in a handler
     * and hand it off to the thread pool.
     */
    @Override
    public void registerNewTask(final String queueMsgId, final byte[] taskMessage, Map<String, Object> headers)
            throws InvalidTaskException, TaskRejectedException {
        Objects.requireNonNull(queueMsgId);


        try {
            TaskMessage tm = codec.deserialise(taskMessage, TaskMessage.class, DecodeMethod.LENIENT);
            messageHandler.handle(tm);
            workerQueue.acknowledgeTask(queueMsgId);
        } catch (CodecException e) {
            throw new TestFailedException("QueueDeliveryHandler failed.", e);

        }
    }

    @Override
    public void abortTasks() {

    }
}
