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
package com.hpe.caf.worker.queue.sqs;

import com.hpe.caf.api.worker.QueueException;
import com.hpe.caf.configs.SQSConfiguration;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.net.URISyntaxException;
import java.util.concurrent.BlockingQueue;

public class SQSWorkerQueueWrapper
{
    final SQSTaskCallback callback;
    final BlockingQueue<CallbackResponse> callbackQueue;
    final SQSWorkerQueueConfiguration sqsWorkerQueueConfiguration;
    final SQSConfiguration sqsConfiguration;
    final SQSWorkerQueue sqsWorkerQueue;
    final SqsClient sqsClient;
    final SQSClientProviderImpl connectionProvider = new SQSClientProviderImpl();

    final String inputQueueUrl;

    public SQSWorkerQueueWrapper(
            final String inputQueue,
            final int visibilityTimeout,
            final int longPollInterval,
            final int maxNumberOfMessages,
            final int maxDeliveries,
            final int messageRetentionPeriod) throws QueueException, URISyntaxException
    {
        callback = new SQSTaskCallback();

        sqsConfiguration = new SQSConfiguration();
        sqsConfiguration.setSqsProtocol("http");
        sqsConfiguration.setSqsHost("localhost");
        sqsConfiguration.setSqsPort(19324);
        sqsConfiguration.setSqsRegion("us-east-1");
        sqsConfiguration.setSqsAccessKey("x");
        sqsConfiguration.setSqsSecretAccessKey("x");

        sqsWorkerQueueConfiguration = new SQSWorkerQueueConfiguration();
        sqsWorkerQueueConfiguration.setSQSConfiguration(sqsConfiguration);
        sqsWorkerQueueConfiguration.setInputQueue(inputQueue);
        sqsWorkerQueueConfiguration.setVisibilityTimeout(visibilityTimeout);
        sqsWorkerQueueConfiguration.setLongPollInterval(longPollInterval);
        sqsWorkerQueueConfiguration.setMaxNumberOfMessages(maxNumberOfMessages);
        sqsWorkerQueueConfiguration.setMessageRetentionPeriod(messageRetentionPeriod);
        sqsWorkerQueueConfiguration.setMaxDeliveries(maxDeliveries);

        sqsWorkerQueue = new SQSWorkerQueue(sqsWorkerQueueConfiguration);
        sqsWorkerQueue.start(callback);

        sqsClient = connectionProvider.getSqsClient(sqsConfiguration);
        callbackQueue = callback.getCallbackQueue();
        inputQueueUrl = SQSUtil.getQueueUrl(sqsClient, sqsWorkerQueueConfiguration.getInputQueue());
    }
}
