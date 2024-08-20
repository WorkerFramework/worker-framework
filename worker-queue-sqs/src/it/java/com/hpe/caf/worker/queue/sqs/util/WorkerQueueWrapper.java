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
package com.hpe.caf.worker.queue.sqs.util;

import com.hpe.caf.worker.queue.sqs.SQSClientProviderImpl;
import com.hpe.caf.worker.queue.sqs.SQSTaskInformation;
import com.hpe.caf.worker.queue.sqs.SQSUtil;
import com.hpe.caf.worker.queue.sqs.SQSWorkerQueue;
import com.hpe.caf.worker.queue.sqs.config.SQSConfiguration;
import com.hpe.caf.worker.queue.sqs.config.SQSWorkerQueueConfiguration;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import static org.junit.jupiter.api.Assertions.fail;


public class WorkerQueueWrapper
{
    final StubbedTaskCallback callback;
    public final BlockingQueue<CallbackResponse> callbackQueue;
    public final BlockingQueue<CallbackResponse> callbackDLQ;
    public final SQSWorkerQueueConfiguration sqsWorkerQueueConfiguration;
    public final SQSConfiguration sqsConfiguration;
    public final SQSWorkerQueue sqsWorkerQueue;
    public final SqsClient sqsClient;
    final SQSClientProviderImpl clientProvider;


    public final String inputQueueUrl;


    // Cloudwatch
    final CloudWatchClient cloudWatch;

    public WorkerQueueWrapper(
            final String inputQueue,
            final int visibilityTimeout,
            final int longPollInterval,
            final int maxNumberOfMessages,
            final int maxDeliveries,
            final int messageRetentionPeriod)
    {
        try {
            callback = new StubbedTaskCallback();

            sqsConfiguration = new SQSConfiguration();
            sqsConfiguration.setAwsProtocol("http");
            sqsConfiguration.setAwsHost("sqs.us-east-1.localhost.localstack.cloud");
            sqsConfiguration.setAwsPort(14566);
            sqsConfiguration.setAwsRegion("us-east-1");
            sqsConfiguration.setAwsAccessKey("x");
            sqsConfiguration.setSecretAccessKey("x");

            clientProvider = new SQSClientProviderImpl(sqsConfiguration);

            sqsWorkerQueueConfiguration = new SQSWorkerQueueConfiguration();
            sqsWorkerQueueConfiguration.setSQSConfiguration(sqsConfiguration);
            sqsWorkerQueueConfiguration.setInputQueue(inputQueue);
            sqsWorkerQueueConfiguration.setRetryQueue(inputQueue);
            sqsWorkerQueueConfiguration.setVisibilityTimeout(visibilityTimeout);
            sqsWorkerQueueConfiguration.setDlqVisibilityTimeout(visibilityTimeout);
            sqsWorkerQueueConfiguration.setLongPollInterval(longPollInterval);
            sqsWorkerQueueConfiguration.setMaxNumberOfMessages(maxNumberOfMessages);
            sqsWorkerQueueConfiguration.setMessageRetentionPeriod(messageRetentionPeriod);
            sqsWorkerQueueConfiguration.setDlqMessageRetentionPeriod(messageRetentionPeriod);
            sqsWorkerQueueConfiguration.setMaxDeliveries(maxDeliveries);

            sqsWorkerQueue = new SQSWorkerQueue(sqsWorkerQueueConfiguration);
            sqsWorkerQueue.start(callback);

            sqsClient = clientProvider.getSqsClient();
            callbackQueue = callback.getCallbackQueue();
            callbackDLQ = callback.getCallbackDLQ();
            inputQueueUrl = SQSUtil.getQueueUrl(sqsClient, sqsWorkerQueueConfiguration.getInputQueue());

            cloudWatch = CloudWatchClient.builder()
                    .credentialsProvider(() -> new AwsCredentials()
                    {
                        @Override
                        public String accessKeyId()
                        {
                            return sqsConfiguration.getAwsAccessKey();
                        }

                        @Override
                        public String secretAccessKey()
                        {
                            return sqsConfiguration.getSecretAccessKey();
                        }
                    })
                    .region(Region.US_EAST_1)
                    .endpointOverride(new URI(sqsConfiguration.getURIString()))
                    .build();
        } catch (final Exception e) {
            throw new RuntimeException("Error starting worker wrapper", e);
        }
    }

    public CloudWatchClient getCloudwatchClient()
    {
        return cloudWatch;
    }

    public static WorkerQueueWrapper getWorkerWrapper(final String inputQueue)
    {
        return getWorkerWrapper(inputQueue, new WrapperConfig());
    }

    public static WorkerQueueWrapper getWorkerWrapper(
            final String inputQueue,
            final WrapperConfig wrapperConfig
    )
    {
        return new WorkerQueueWrapper(
                inputQueue,
                wrapperConfig.visibilityTimout(),
                wrapperConfig.longPollInterval(),
                wrapperConfig.maxReadMessages(),
                wrapperConfig.maxDeliveries(),
                wrapperConfig.retentionPeriod());
    }

    public static void sendMessages(
            final WorkerQueueWrapper workerQueueWrapper,
            final String... messages
    )
    {
        try {
            for(final String message : messages) {
                sendMessage(
                        workerQueueWrapper.sqsClient,
                        workerQueueWrapper.inputQueueUrl,
                        new HashMap<>(),
                        message
                );
            }
        } catch (final Exception e) {
            fail("Failed sending messages " + e.getMessage());
        }
    }

    public static void sendMessages(
            final SqsClient sqsClient,
            final String queueUrl,
            final Map<String, MessageAttributeValue> messageAttributes,
            final String... messages)
    {
        try {
            for(final String message : messages) {
                sendMessage(sqsClient, queueUrl, messageAttributes, message);
            }
        } catch (final Exception e) {
            fail("Failed sending messages " + e.getMessage());
        }
    }

    public static void sendMessage(
            final SqsClient sqsClient,
            final String queueUrl,
            final Map<String, MessageAttributeValue> messageAttributes,
            final String message)
    {
        try {
            final var sendRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(message)
                    .messageAttributes(messageAttributes)
                    .build();
            sqsClient.sendMessage(sendRequest);
        } catch (final Exception e) {
            fail("Failed sending message " + e.getMessage());
        }
    }

    public static void sendMessagesInBatches(
            final SqsClient sqsClient,
            final String queueUrl,
            final int numMessages)
    {
        try {
            for(int j = 0; j < numMessages / 10; j++) {
                var entries = new ArrayList<SendMessageBatchRequestEntry>();
                for(int i = 1; i <= 10; i++) {
                    final var msg = String.format("msg-%d-%d", j, i);
                    var entry = SendMessageBatchRequestEntry.builder()
                            .id(msg)
                            .delaySeconds(0)
                            .messageBody(msg)
                            .build();
                    entries.add(entry);
                }
                final var sendRequest = SendMessageBatchRequest.builder()
                        .queueUrl(queueUrl)
                        .entries(entries)
                        .build();
                sqsClient.sendMessageBatch(sendRequest);
            }

        } catch (final Exception e) {
            fail("Failed sending messages " + e.getMessage());
        }
    }

    public static void sendSingleMessagesWithDelays(
            final SqsClient sqsClient,
            final String queueUrl,
            final int numMessages,
            final long delay)
    {
        try {
            for(int j = 0; j < numMessages; j++) {
                sendMessage(sqsClient, queueUrl, new HashMap<>(), String.valueOf(j));
                Thread.sleep(delay * 1000);
            }

        } catch (final Exception e) {
            fail("Failed sending message " + e.getMessage());
        }
    }

    public static void deleteMessage(
            final SqsClient sqsClient,
            final SQSTaskInformation taskInfo)
    {
        final var sendRequest = DeleteMessageRequest.builder()
                .queueUrl(taskInfo.getQueueInfo().url())
                .receiptHandle(taskInfo.getReceiptHandle())
                .build();
        sqsClient.deleteMessage(sendRequest);
    }

    public static void purgeQueue(
            final SqsClient sqsClient,
            final String queueUrl)
    {
        final var purgeQueueRequest = PurgeQueueRequest.builder()
                .queueUrl(queueUrl)
                .build();
        sqsClient.purgeQueue(purgeQueueRequest);

        final var purgeDeadLetterQueueRequest = PurgeQueueRequest.builder()
                .queueUrl(queueUrl + SQSUtil.DEAD_LETTER_QUEUE_SUFFIX)
                .build();
        try {
            sqsClient.purgeQueue(purgeDeadLetterQueueRequest);
        } catch (final QueueDoesNotExistException e) {
            // Ignoring this
        }
    }
}
