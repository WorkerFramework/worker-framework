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
package com.hpe.caf.worker.queue.rabbit;

import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.util.rabbitmq.*;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Recoverable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;

/**
 * This implementation uses a separate thread for a consumer and producer, each with their own Channel. These threads handle operations
 * via a BlockingQueue of Event objects. In all scenarios where the tasks triggered by the message take significantly longer than the
 * handling of the messages themselves (which should hopefully be true of all microservices), this implementation should work.
 *
 * This implementation has three routing keys, assumed to be on a direct exchange (hence effectively being queue names). There is the
 * input queue to receive messages from, the retry queue (which may be the input queue) where redelivered messages get republished to, and
 * the rejected queue which is where messages that could not be handled are put. There are an unlimited number of possible output queues
 * as defined by the Worker's response. All published messages use RabbitMQ confirmations.
 */
public final class RabbitWorkerQueue implements ManagedWorkerQueue
{
    private DefaultRabbitConsumer consumer;
    private EventPoller<WorkerPublisher> publisher;
    private Connection conn;
    private Channel incomingChannel;
    private Channel outgoingChannel;
    private Thread publisherThread;
    private Thread consumerThread;
    private String consumerTag;
    private final Object consumerLock = new Object();
    private final Set<String> declaredQueues = new HashSet<>();
    private final BlockingQueue<Event<QueueConsumer>> consumerQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Event<WorkerPublisher>> publisherQueue = new LinkedBlockingQueue<>();
    private final RabbitMetricsReporter metrics = new RabbitMetricsReporter();
    private final RabbitWorkerQueueConfiguration config;
    private final int maxTasks;
    private static final Logger LOG = LoggerFactory.getLogger(RabbitWorkerQueue.class);

    /**
     * Setup a new RabbitWorkerQueue.
     */
    public RabbitWorkerQueue(RabbitWorkerQueueConfiguration config, int maxTasks)
    {
        this.config = Objects.requireNonNull(config);
        this.maxTasks = maxTasks;
        LOG.debug("Initialised");
    }

    /**
     * {@inheritDoc}
     *
     * Create a RabbitMQ connection, and separate incoming and outgoing channels. The connection and channels will
     * attempt to re-establish should they drop. Declare the queues on the appropriate channels and kick off the publisher and consumer
     * threads to handle messages. Since this code uses publisher confirms, it is important currently to declare the publisher channel
     * before the consumer channel, otherwise during a connection drop scenario, the client can report the publish sequence number for the "old"
     * channel before recovering it.
     */
    @Override
    public void start(TaskCallback callback)
        throws QueueException
    {
        if (conn != null) {
            throw new IllegalStateException("Already started");
        }
        try {
            WorkerConfirmListener confirmListener = new WorkerConfirmListener(consumerQueue);
            createConnection(callback, confirmListener);
            outgoingChannel = conn.createChannel();
            incomingChannel = conn.createChannel();
            int prefetch = Math.max(1, maxTasks + config.getPrefetchBuffer());
            incomingChannel.basicQos(prefetch);
            WorkerQueueConsumerImpl consumerImpl = new WorkerQueueConsumerImpl(callback, metrics, consumerQueue, incomingChannel,
                                                                               publisherQueue, config.getRetryQueue(), config.getRetryLimit());
            consumer = new DefaultRabbitConsumer(consumerQueue, consumerImpl);
            WorkerPublisherImpl publisherImpl = new WorkerPublisherImpl(outgoingChannel, metrics, consumerQueue, confirmListener);
            publisher = new EventPoller<>(2, publisherQueue, publisherImpl);
            declareWorkerQueue(incomingChannel, config.getInputQueue());
            declareWorkerQueue(outgoingChannel, config.getRetryQueue());
            synchronized (consumerLock) {
                consumerTag = incomingChannel.basicConsume(config.getInputQueue(), consumer);
            }
        } catch (final IOException | TimeoutException | URISyntaxException | NoSuchAlgorithmException | 
                       KeyManagementException e) 
        {
            throw new QueueException("Failed to establish queues", e);
        }
        publisherThread = new Thread(publisher);
        consumerThread = new Thread(consumer);
        publisherThread.start();
        consumerThread.start();
    }

    @Override
    public void publish(TaskInformation taskInformation, byte[] taskMessage, String targetQueue, Map<String, Object> headers,
                        boolean isLastMessage) throws QueueException
    {
        try {
            declareWorkerQueue(outgoingChannel, targetQueue);
        } catch (IOException e) {
            throw new QueueException("Failed to submit task", e);
        }
        RabbitTaskInformation rabbitTaskInformation = (RabbitTaskInformation)taskInformation;
        //increment the total responseCount (including task, sub task and tracking info)
        rabbitTaskInformation.incrementResponseCount(isLastMessage);
        publisherQueue.add(new WorkerPublishQueueEvent(taskMessage, targetQueue, rabbitTaskInformation, headers));
    }
    
    @Override
    public void publish(TaskInformation taskInformation, byte[] taskMessage, String targetQueue, Map<String, Object> headers) throws QueueException
    {
        publish(taskInformation, taskMessage, targetQueue, headers, false);
    }

    /**
     * {@inheritDoc}
     *
     * Add a REJECT event that the consumer will handle.
     */
    @Override
    public void rejectTask(TaskInformation taskInformation)
    {
        Objects.requireNonNull(taskInformation);
        LOG.debug("Generating reject event for task {}", taskInformation.getInboundMessageId());
        consumerQueue.add(new ConsumerRejectEvent(Long.parseLong(taskInformation.getInboundMessageId())));
    }

    /**
     * {@inheritDoc}
     *
     * Add a DROP event that the consumer will handle.
     */
    @Override
    public void discardTask(TaskInformation taskInformation)
    {
        Objects.requireNonNull(taskInformation);
        LOG.debug("Generating drop event for task {}", taskInformation.getInboundMessageId());
        consumerQueue.add(new ConsumerDropEvent(Long.parseLong(taskInformation.getInboundMessageId())));
    }

    /**
     * {@inheritDoc}
     *
     * Add a ACKNOWLEDGE event that consumer will handle.
     */
    @Override
    public void acknowledgeTask(TaskInformation taskInformation)
    {
        Objects.requireNonNull(taskInformation);
        LOG.debug("Generating acknowledge event for task {}", taskInformation.getInboundMessageId());
        consumerQueue.add(new ConsumerAckEvent(Long.parseLong(taskInformation.getInboundMessageId())));
    }

    /**
     * {@inheritDoc}
     *
     * Return the name of the input queue.
     */
    @Override
    public String getInputQueue()
    {
        return config.getInputQueue();
    }

    /**
     * {@inheritDoc}
     *
     * Return the name of the paused queue.
     */
    @Override
    public String getPausedQueue()
    {
        return config.getPausedQueue();
    }

    /**
     * {@inheritDoc}
     *
     * The incoming queues will all be cancelled so the consumer will fall back to idle.
     */
    @Override
    public void shutdownIncoming()
    {
        LOG.debug("Closing incoming queues");
        synchronized (consumerLock) {
            if (consumerTag != null) {
                try {
                    incomingChannel.basicCancel(consumerTag);
                    consumerTag = null;
                } catch (IOException e) {
                    metrics.incremementErrors();
                    LOG.warn("Failed to cancel consumer {}", consumerTag, e);
                    if(e.getMessage().contains("Unknown consumerTag")) {
                        attemptMessageRecovery();
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * This method can be used to stop a worker consuming messages from it's input queue.
     *
     * This is useful in, (for example), an instance where a worker's health check has failed.
     *
     * The worker is disconnected from the incoming channel and the consumerTag removed from the list of consumerTags.
     */
    @Override
    public void disconnectIncoming()
    {
        LOG.debug("Disconnecting incoming queues");
        synchronized (consumerLock) {
            if (consumerTag != null && incomingChannel.isOpen()) {
                try {
                    incomingChannel.basicCancel(consumerTag);
                    consumerTag = null;
                } catch (IOException ioe) {
                    LOG.error("Failed to cancel consumer {}", consumerTag, ioe);
                    if(ioe.getMessage().contains("Unknown consumerTag")) {
                        attemptMessageRecovery();
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * This method can be used to reconnect a worker to it's input queue therefore allowing to resume consuming messages.
     *
     * This is useful in, (for example), an instance where a worker's health check indicates the worker has become healthy again and
     * should resume consuming messages.
     *
     * The worker is reconnected to the incoming channel and the returned consumerTag added to the list of consumerTags.
     */
    @Override
    public void reconnectIncoming()
    {
        LOG.debug("Reconnecting incoming queues");
        synchronized (consumerLock) {
            if (consumerTag == null && incomingChannel.isOpen()) {
                try {
                    consumerTag = incomingChannel.basicConsume(config.getInputQueue(), consumer);
                } catch (IOException ioe) {
                    LOG.error("Failed to reconnect consumer {}", ioe);
                }
            }
        }
    }

    @Override
    public void shutdown()
    {
        LOG.debug("Shutting down");
        try {
            if (consumer != null) {
                consumer.shutdown();
            }
            if (publisher != null) {
                publisher.shutdown();
            }
            if (conn != null) {
                incomingChannel.close();
                outgoingChannel.close();
                conn.close();
            }
        } catch (IOException | TimeoutException e) {
            metrics.incremementErrors();
            LOG.warn("Failed to close rabbit connections", e);
        }
    }

    @Override
    public WorkerQueueMetricsReporter getMetrics()
    {
        return metrics;
    }

    @Override
    public HealthResult healthCheck()
    {
        if (!conn.isOpen()) {
            return new HealthResult(HealthStatus.UNHEALTHY, "Rabbit connection failed");
        } else if (!incomingChannel.isOpen()) {
            return new HealthResult(HealthStatus.UNHEALTHY, "Incoming channel failed");
        } else if (!outgoingChannel.isOpen()) {
            return new HealthResult(HealthStatus.UNHEALTHY, "Outgoing channel failed");
        } else if (consumerThread == null || !consumerThread.isAlive()) {
            return new HealthResult(HealthStatus.UNHEALTHY, "RabbitMQ listening thread not running");
        } else if (publisherThread == null || !publisherThread.isAlive()) {
            return new HealthResult(HealthStatus.UNHEALTHY, "RabbitMQ publishing thread not running");
        } else {
            return HealthResult.RESULT_HEALTHY;
        }
    }

    private void createConnection(TaskCallback callback, WorkerConfirmListener listener)
        throws IOException, TimeoutException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException
    {
        conn = RabbitUtil.createRabbitConnection(config.getRabbitConfiguration());
        ((Recoverable)conn).addRecoveryListener(new WorkerConnectionListener(callback, listener));
    }

    private void declareWorkerQueue(Channel channel, String queueName)
        throws IOException
    {
        if (!declaredQueues.contains(queueName)) {

            RabbitUtil.declareWorkerQueue(channel, queueName, config.getMaxPriority(), config.getQueueType());
            declaredQueues.add(queueName);
        }
    }

    private void attemptMessageRecovery()
    {
        synchronized (consumerLock) {
            try {
                LOG.warn("Attempting to recover unack'd messages from consumer {}", consumer);
                incomingChannel.basicRecover();
            } catch (final IOException e) {
                metrics.incremementErrors();
                LOG.warn("Failed to recover messages from consumer {}", consumerTag, e);
            }
        }
    }

}
