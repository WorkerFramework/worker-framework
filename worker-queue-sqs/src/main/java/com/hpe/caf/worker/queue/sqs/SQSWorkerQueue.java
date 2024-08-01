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

import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.worker.ManagedWorkerQueue;
import com.hpe.caf.api.worker.QueueException;
import com.hpe.caf.api.worker.TaskCallback;
import com.hpe.caf.api.worker.TaskInformation;
import com.hpe.caf.api.worker.WorkerQueueMetricsReporter;
import com.hpe.caf.configs.SQSConfiguration;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import software.amazon.awssdk.awscore.exception.*;
import software.amazon.awssdk.core.exception.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

public final class SQSWorkerQueue implements ManagedWorkerQueue
{
    private SqsClient sqsClient;
    private final SQSWorkerQueueConfiguration sqsQueueConfiguration;
    private final SQSConfiguration sqsConfiguration;
    private final Map<String, String> declaredQueues = new ConcurrentHashMap<>();
    private final BlockingQueue<SQSEvent<SQSQueueConsumer>> consumerQueue = new LinkedBlockingQueue<>();

    private static final Logger LOG = LoggerFactory.getLogger(SQSWorkerQueue.class);
    private static final SqsClientProviderImpl connectionProvider = new SqsClientProviderImpl();

    public SQSWorkerQueue(final SQSWorkerQueueConfiguration sqsQueueConfiguration)
    {
        this.sqsQueueConfiguration = Objects.requireNonNull(sqsQueueConfiguration);
        sqsConfiguration = sqsQueueConfiguration.getSQSConfiguration();
    }

    public void start(final TaskCallback callback) throws QueueException
    {
        try
        {
            sqsClient = connectionProvider.getSqsClient(sqsConfiguration);
            createQueue(sqsQueueConfiguration.getInputQueue());
            createQueue(sqsQueueConfiguration.getRetryQueue());
        } catch (final Exception e)
        {
            throw new QueueException("Failed to start worker queue", e);
        }
    }

    @Override
    public void shutdownIncoming()
    {

    }

    @Override
    public void shutdown()
    {

    }

    @Override
    public WorkerQueueMetricsReporter getMetrics()
    {
        return null;
    }

    @Override
    public void disconnectIncoming()
    {

    }

    @Override
    public void reconnectIncoming()
    {

    }

    /**
     *
     * @param queueName
     * @return
     */
    private String createQueue(final String queueName)
    {
        if (!declaredQueues.containsKey(queueName))
        {
            // DDD what else is required here (FIFO etc?)
            final var attributes = new HashMap<QueueAttributeName, String>();
            attributes.put(
                    QueueAttributeName.VISIBILITY_TIMEOUT,
                    String.valueOf(sqsQueueConfiguration.getVisibilityTimeout())
            );
            final var createQueueRequest = CreateQueueRequest.builder()
                    .queueName(queueName)
                    .attributes(attributes)
                    .build();
            try
            {
                final var response = sqsClient.createQueue(createQueueRequest);
                declaredQueues.put(queueName, response.queueUrl());
            } catch (final QueueNameExistsException e)
            {
                LOG.info("Queue already exists {} {}", queueName, e.getMessage());
                declaredQueues.put(queueName, SQSUtil.getQueueUrl(sqsClient, queueName));
            }
        }
        return declaredQueues.get(queueName);
    }

    @Override
    public HealthResult healthCheck()
    {
        return null;
    }

    @Override
    public void publish(
            final TaskInformation taskInformation,
            final byte[] taskMessage,
            final String targetQueue,
            final Map<String, Object> headers, // DDD unused ?
            final boolean isLastMessage // DDD unused ?
    ) throws QueueException
    {
        try
        {
            final var queueUrl = createQueue(targetQueue);
            final var sendMsgRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(new String(taskMessage, StandardCharsets.UTF_8))
                    .build();

            sqsClient.sendMessage(sendMsgRequest);
        } catch (final Exception e)
        {
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
    public void rejectTask(final TaskInformation taskInformation)
    {

    }

    @Override
    public void discardTask(TaskInformation taskInformation)
    {

    }

    @Override
    public void acknowledgeTask(TaskInformation taskInformation)
    {

    }

    @Override
    public String getInputQueue()
    {
        return sqsQueueConfiguration.getInputQueue();
    }

    @Override
    public String getPausedQueue()
    {
        return sqsQueueConfiguration.getPausedQueue();
    }
}