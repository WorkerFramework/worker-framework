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
package com.hpe.caf.worker.core;

import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.ManagedWorkerQueue;
import com.hpe.caf.api.worker.QueueException;
import com.hpe.caf.api.worker.WorkerQueueProvider;
import com.hpe.caf.worker.queue.rabbit.RabbitWorkerQueue;
import com.hpe.caf.worker.queue.rabbit.RabbitWorkerQueueConfiguration;
import com.hpe.caf.worker.queue.sqs.SQSWorkerQueue;
import com.hpe.caf.worker.queue.sqs.config.SQSWorkerQueueConfiguration;

public class WorkerQueueProviderImpl implements WorkerQueueProvider
{
    private static final String AWS_SQS_MESSAGING = "sqs";

    @Override
    public ManagedWorkerQueue getWorkerQueue(ConfigurationSource configurationSource, int maxTasks) throws QueueException
    {
        try {
            final var messageSystemCfg = configurationSource.getConfiguration(MessageSystemConfiguration.class);
            if (messageSystemCfg.getImplementation().equals(AWS_SQS_MESSAGING)) {
                return new SQSWorkerQueue(configurationSource.getConfiguration(SQSWorkerQueueConfiguration.class), maxTasks);
            }
            return new RabbitWorkerQueue(configurationSource.getConfiguration(RabbitWorkerQueueConfiguration.class), maxTasks);
        } catch (ConfigurationException e) {
            throw new QueueException("Cannot create worker queue", e);
        }
    }
}
