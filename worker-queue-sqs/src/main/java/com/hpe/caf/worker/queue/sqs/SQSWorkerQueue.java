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

import com.amazon.sqs.javamessaging.AmazonSQSMessagingClientWrapper;
import com.amazon.sqs.javamessaging.ProviderConfiguration;
import com.amazon.sqs.javamessaging.SQSConnectionFactory;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.worker.ManagedWorkerQueue;
import com.hpe.caf.api.worker.QueueException;
import com.hpe.caf.api.worker.TaskCallback;
import com.hpe.caf.api.worker.TaskInformation;
import com.hpe.caf.api.worker.WorkerQueueMetricsReporter;
import com.hpe.caf.configs.SQSConfiguration;

import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.JMSException;

public final class SQSWorkerQueue implements ManagedWorkerQueue
{
    private final SQSWorkerQueueConfiguration sqsQueueConfiguration;
    private final SQSConfiguration sqsConfiguration;
    private AmazonSQSMessagingClientWrapper client;
    private final ConcurrentHashMap<String, String> declaredQueues = new ConcurrentHashMap<>();

    private static final Logger LOG = LoggerFactory.getLogger(SQSWorkerQueue.class);

    public SQSWorkerQueue(final SQSWorkerQueueConfiguration sqsQueueConfiguration) {
        this.sqsQueueConfiguration = Objects.requireNonNull(sqsQueueConfiguration);
        sqsConfiguration = sqsQueueConfiguration.getSQSConfiguration();
    }

    public void start(final TaskCallback callback) throws QueueException {
        try {
            client = createSQSClient();
            createQueue(sqsQueueConfiguration.getInputQueue());
            createQueue(sqsQueueConfiguration.getRetryQueue());
        } catch (final Exception e) {
            throw new QueueException("Failed to create sqs client", e);
        }
    }

    private AmazonSQSMessagingClientWrapper createSQSClient() throws JMSException {
        final var endpoint = new AwsClientBuilder.EndpointConfiguration(
                sqsConfiguration.getURIString(), sqsConfiguration.getSqsRegion());

        final var connectionFactory = new SQSConnectionFactory(
                new ProviderConfiguration(),
                AmazonSQSClientBuilder.standard()
                        .withEndpointConfiguration(endpoint)
                        .withCredentials(new AWSCredentialsProvider() {
                            @Override
                            public AWSCredentials getCredentials() {
                                return new AWSCredentials() {
                                    @Override
                                    public String getAWSAccessKeyId() {
                                        return sqsConfiguration.getSqsAccessKey();
                                    }

                                    @Override
                                    public String getAWSSecretKey() {
                                        return sqsConfiguration.getSqsSecretAccessKey();
                                    }
                                };
                            }

                            @Override
                            public void refresh() {

                            }
                        })

        );

        final var connection = connectionFactory.createConnection();
        return connection.getWrappedAmazonSQSClient();
    }

    @Override
    public void shutdownIncoming() {

    }

    @Override
    public void shutdown() {

    }

    @Override
    public WorkerQueueMetricsReporter getMetrics() {
        return null;
    }

    @Override
    public void disconnectIncoming() {

    }

    @Override
    public void reconnectIncoming() {

    }

    public String createQueue(final String queueName) throws QueueException {
        if (!declaredQueues.containsKey(queueName)) {
            // DDD whats required here (FIFO etc?)
            final var attributes = new HashMap<String, String>();

            try {
                final com.amazonaws.services.sqs.model.CreateQueueRequest createRequest = new com.amazonaws.services.sqs.model.CreateQueueRequest()
                        .withQueueName(queueName)
                        .withAttributes(attributes);
                if (!client.queueExists(queueName)) {
                    var queueUrl = client.createQueue(createRequest).getQueueUrl();
                    declaredQueues.put(queueName, queueUrl);
                    return queueUrl;
                }
            } catch (final Exception e) {
                LOG.error("Error creating queue {} {}", queueName, e.getMessage());
                throw new QueueException("Error creating queue", e);
            }
        }
        return declaredQueues.get(queueName);
    }

    @Override
    public HealthResult healthCheck() {
        return null;
    }

    @Override
    public void publish(
            final TaskInformation taskInformation,
            final byte[] taskMessage,
            final String targetQueue,
            final Map<String, Object> headers, // DDD unused
            final boolean isLastMessage // DDD unused
    ) throws QueueException
    {
        try {
            var queueUrl = createQueue(targetQueue);
            final var sendMsgRequest = new com.amazonaws.services.sqs.model.SendMessageRequest()
                    .withQueueUrl(queueUrl)
                    .withMessageBody(new String(taskMessage, StandardCharsets.UTF_8));

            client.sendMessage(sendMsgRequest);
        } catch (final Exception e) {
            LOG.error("Error publishing task message {} {}", taskInformation.getInboundMessageId(), e.getMessage());
            throw new QueueException("Error publishing task message", e);
        }
    }

    @Override
    public void publish(
            final TaskInformation taskInformation,
            final byte[] taskMessage,
            final String targetQueue,
            final Map<String, Object> headers) throws QueueException
    {
        publish(taskInformation, taskMessage, targetQueue, headers, false);
    }

    @Override
    public void rejectTask(final TaskInformation taskInformation) {

    }

    @Override
    public void discardTask(TaskInformation taskInformation) {

    }

    @Override
    public void acknowledgeTask(TaskInformation taskInformation) {

    }

    @Override
    public String getInputQueue() {
        return sqsQueueConfiguration.getInputQueue();
    }

    @Override
    public String getPausedQueue() {
        return sqsQueueConfiguration.getPausedQueue();
    }

    public Map<String, String> getDeclaredQueues() {
        return declaredQueues;
    }

    private String getQueueUrl(final String queueName) throws QueueException {
        final var getQueueUrlRequest = new com.amazonaws.services.sqs.model.GetQueueUrlRequest()
                .withQueueName(queueName);
        try {
            final var queueUrl = client.getQueueUrl(getQueueUrlRequest).getQueueUrl();
            declaredQueues.put(queueName, queueUrl);
            return queueUrl;
        } catch (final Exception e) {
            LOG.error("Unable to read queue url {} {}", queueName, e.getMessage());
            throw new QueueException("Unable to read queue url", e);
        }
    }
}
