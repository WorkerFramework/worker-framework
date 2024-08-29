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
import com.hpe.caf.worker.queue.sqs.util.SQSMetricsReporter;
import com.hpe.caf.worker.queue.sqs.visibility.VisibilityMonitor;
import software.amazon.awssdk.services.sqs.SqsClient;

public class InputQueueConsumer extends QueueConsumer
{

    private final VisibilityMonitor visibilityMonitor;

    public InputQueueConsumer(
            final SqsClient sqsClient,
            final QueueInfo queueInfo,
            final QueueInfo retryQueueInfo,
            final TaskCallback callback,
            final SQSWorkerQueueConfiguration queueCfg,
            final VisibilityMonitor visibilityMonitor,
            final SQSMetricsReporter sqsMetricsReporter)
    {
        super(sqsClient, queueInfo, retryQueueInfo, queueCfg, callback, sqsMetricsReporter);
        this.visibilityMonitor = visibilityMonitor;
    }

    @Override
    protected void handleRegistrationTasks(final SQSTaskInformation taskInfo)
    {
        sqsMetricsReporter.incrementReceived();
        visibilityMonitor.watch(taskInfo);
    }

    @Override
    protected int getVisibilityTimeout()
    {
        return queueCfg.getVisibilityTimeout();
    }

    @Override
    protected boolean isPoisonMessageConsumer()
    {
        return false;
    }
}
