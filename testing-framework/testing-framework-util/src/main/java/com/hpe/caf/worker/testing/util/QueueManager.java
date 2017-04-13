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
import com.hpe.caf.api.worker.ManagedWorkerQueue;
import com.hpe.caf.api.worker.QueueException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.worker.testing.api.TaskMessageHandler;

import java.util.HashMap;

/**
 * Created by ploch on 06/03/2017.
 */
public class QueueManager
{

    private final Codec codec;

    private final ManagedWorkerQueue workerQueue;
    private final String targetQueueName;

    public QueueManager(Codec codec, ManagedWorkerQueue workerQueue, String targetQueueName)
    {
        this.codec = codec;
        this.workerQueue = workerQueue;
        this.targetQueueName = targetQueueName;
    }

    /*public QueueManager(Codec codec, ConfigurationSource configurationSource) throws QueueException, ModuleLoaderException {
        this.codec = codec;

        WorkerQueueProvider provider = ModuleLoader.getService(WorkerQueueProvider.class);
        workerQueue = provider.getWorkerQueue(configurationSource, 1);

    }*/

    public void start(TaskMessageHandler taskMessageHandler) throws QueueException
    {
        workerQueue.start(new QueueDeliveryHandler(taskMessageHandler, workerQueue, codec));

    }

    public void publish(TaskMessage taskMessage) throws CodecException, QueueException
    {
        workerQueue.publish("1", codec.serialise(taskMessage), targetQueueName, new HashMap<>());
    }

    public ManagedWorkerQueue getWorkerQueue()
    {
        return workerQueue;
    }
}
