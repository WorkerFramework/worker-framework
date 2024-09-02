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
package com.hpe.caf.worker.queue.sqs.consumer;

import com.hpe.caf.api.worker.TaskCallback;
import com.hpe.caf.worker.queue.sqs.QueueInfo;
import com.hpe.caf.worker.queue.sqs.SQSTaskInformation;
import com.hpe.caf.worker.queue.sqs.config.SQSWorkerQueueConfiguration;
import com.hpe.caf.worker.queue.sqs.metrics.MetricsReporter;
import com.hpe.caf.worker.queue.sqs.visibility.VisibilityMonitor;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.util.concurrent.atomic.AtomicBoolean;

public class DeadLetterQueueConsumer extends QueueConsumer
{
    public DeadLetterQueueConsumer(
            final SqsClient sqsClient,
            final QueueInfo queueInfo,
            final QueueInfo retryQueueInfo,
            final TaskCallback callback,
            final SQSWorkerQueueConfiguration queueCfg,
            final VisibilityMonitor visibilityMonitor,
            final MetricsReporter metricsReporter,
            final AtomicBoolean receiveMessages)
    {
        super(sqsClient, queueInfo, retryQueueInfo, queueCfg, callback,
                visibilityMonitor, metricsReporter, receiveMessages);
    }

    @Override
    protected void handleConsumerSpecificActions(final SQSTaskInformation taskInfo)
    {
        // DDD does the poison message get acked by the implementation, if not do so here.
        // else remove this method.
    }

    @Override
    protected boolean isPoisonMessageConsumer()
    {
        return true;
    }
}
