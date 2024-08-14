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
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

import static org.testng.AssertJUnit.fail;

public class SQSWorkerQueueWrapper
{
    final SQSTaskCallback callback;
    final BlockingQueue<CallbackResponse> callbackQueue;
    final BlockingQueue<CallbackResponse> callbackDLQ;
    final SQSWorkerQueueConfiguration sqsWorkerQueueConfiguration;
    final SQSConfiguration sqsConfiguration;
    final SQSWorkerQueue sqsWorkerQueue;
    final SqsClient sqsClient;
    final SQSClientProviderImpl clientProvider;

    final String inputQueueUrl;


    // Cloudwatch
    final CloudWatchClient cloudWatch;

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
        sqsConfiguration.setAwsProtocol("http");
        sqsConfiguration.setAwsHost("sqs.us-east-1.localhost.localstack.cloud");
        sqsConfiguration.setAwsPort(4566);
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

        try {
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
            throw new RuntimeException(e);
        }
    }

    public CloudWatchClient getCloudwatchClient()
    {
        return cloudWatch;
    }

    static SQSWorkerQueueWrapper getWorkerWrapper(
            final String inputQueue,
            final int visibilityTimeout,
            final int longPollInterval,
            final int maxNumberOfMessages,
            final int maxDeliveries,
            final int messageRetentionPeriod
    ) throws Exception
    {
        return new SQSWorkerQueueWrapper(
                inputQueue,
                visibilityTimeout,
                longPollInterval,
                maxNumberOfMessages,
                maxDeliveries,
                messageRetentionPeriod);
    }

    public static void sendMessages(
            final SQSWorkerQueueWrapper sqsWorkerQueueWrapper,
            final String... messages
    )
    {
        try {
            for (final String message : messages) {
                sendMessage(
                        sqsWorkerQueueWrapper.sqsClient,
                        sqsWorkerQueueWrapper.inputQueueUrl,
                        new HashMap<>(),
                        message
                );
            }
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }

//    public static void sendMessages(
//            final SqsClient sqsClient,
//            final String queueUrl,
//            final String... messages)
//    {
//        try {
//            for (final String message : messages) {
//                sendMessage(sqsClient, queueUrl, new HashMap<>(), message);
//            }
//        } catch (final Exception e) {
//            fail(e.getMessage());
//        }
//    }

    public static void sendMessages(
            final SqsClient sqsClient,
            final String queueUrl,
            final Map<String, MessageAttributeValue> messageAttributes,
            final String... messages)
    {
        try {
            for (final String message : messages) {
                sendMessage(sqsClient, queueUrl, messageAttributes, message);
            }
        } catch (final Exception e) {
            fail(e.getMessage());
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
            fail(e.getMessage());
        }
    }

    public static void sendMessageBatch(
            final SqsClient sqsClient,
            final String queueUrl,
            final List<SendMessageBatchRequestEntry> entries)
    {
        try {
            final var sendRequest = SendMessageBatchRequest.builder()
                    .queueUrl(queueUrl)
                    .entries(entries)
                    .build();
            sqsClient.sendMessageBatch(sendRequest);
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }

    static void purgeQueue(
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
