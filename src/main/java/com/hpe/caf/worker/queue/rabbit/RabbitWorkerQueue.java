package com.hpe.caf.worker.queue.rabbit;


import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import com.hpe.caf.api.worker.ManagedWorkerQueue;
import com.hpe.caf.api.worker.QueueException;
import com.hpe.caf.api.worker.TaskCallback;
import com.hpe.caf.api.worker.WorkerQueueMetricsReporter;
import com.hpe.caf.configs.RabbitConfiguration;
import com.hpe.caf.util.rabbitmq.ConsumerRejectEvent;
import com.hpe.caf.util.rabbitmq.DefaultRabbitConsumer;
import com.hpe.caf.util.rabbitmq.Event;
import com.hpe.caf.util.rabbitmq.EventPoller;
import com.hpe.caf.util.rabbitmq.QueueConsumer;
import com.hpe.caf.util.rabbitmq.RabbitUtil;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import net.jodah.lyra.ConnectionOptions;
import net.jodah.lyra.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeoutException;


/**
 * This implementation uses a separate thread for a consumer and producer, each with their own Channel.
 * These threads handle operations via a BlockingQueue of Event objects. In all scenarios where the
 * tasks triggered by the message take significantly longer than the handling of the messages themselves
 * (which should hopefully be true of all microservices), this implementation should work.
 *
 * This implementation has three routing keys, assumed to be on a direct exchange (hence effectively being
 * queue names). There is the input queue to receive messages from, the retry queue (which may be the input
 * queue) where redelivered messages get republished to, and the rejected queue which is where messages that
 * could not be handled are put. There are an unlimited number of possible output queues as defined by the
 * Worker's response.
 * @since 7.5
 */
public final class RabbitWorkerQueue implements ManagedWorkerQueue
{
    private DefaultRabbitConsumer consumer;
    private EventPoller<WorkerPublisher> publisher;
    private Connection conn;
    private Channel incomingChannel;
    private Channel outgoingChannel;
    private final List<String> consumerTags = new LinkedList<>();
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
     * Create a RabbitMQ connection, and separate incoming and outgoing channels. The connection and channels are managed by Lyra, so
     * will attempt to re-establish should they drop.
     * Declare the queues on the appropriate channels and kick off the publisher and consumer threads to handle messages.
     */
    @Override
    public void start(TaskCallback callback)
        throws QueueException
    {
        if ( conn != null ) {
            throw new IllegalStateException("Already started");
        }
        try {
            createConnection(callback);
            incomingChannel = conn.createChannel();
            int prefetch = Math.max(1, maxTasks + config.getPrefetchBuffer());
            incomingChannel.basicQos(prefetch);
            outgoingChannel = conn.createChannel();
            WorkerQueueConsumerImpl consumerImpl = new WorkerQueueConsumerImpl(callback, metrics, consumerQueue, incomingChannel, publisherQueue,
                                                                               config.getRetryQueue(), config.getRejectedQueue(), config.getRetryLimit());
            consumer = new DefaultRabbitConsumer(consumerQueue, consumerImpl);
            WorkerPublisherImpl publisherImpl = new WorkerPublisherImpl(outgoingChannel, metrics, consumerQueue);
            publisher = new EventPoller<>(2, publisherQueue, publisherImpl);
            declareWorkerQueue(incomingChannel, config.getInputQueue());
            declareWorkerQueue(outgoingChannel, config.getRetryQueue());
            declareWorkerQueue(outgoingChannel, config.getRejectedQueue());
            consumerTags.add(incomingChannel.basicConsume(config.getInputQueue(), consumer));
        } catch (IOException | TimeoutException e) {
            throw new QueueException("Failed to establish queues", e);
        }
        new Thread(publisher).start();
        new Thread(consumer).start();
    }


    /**
     * {@inheritDoc}
     *
     * Add a PUBLISH event that the publisher thread will handle.
     */
    @Override
    public void publish(String acknowledgeId, byte[] taskMessage, String targetQueue)
        throws QueueException
    {
        try {
            declareWorkerQueue(outgoingChannel, targetQueue);
        } catch (IOException e) {
            throw new QueueException("Failed to submit task", e);
        }
        publisherQueue.add(new WorkerPublishQueueEvent(taskMessage, targetQueue, Long.parseLong(acknowledgeId)));
    }


    /**
     * {@inheritDoc}
     *
     * Add a REJECT event that the consumer will handle.
     */
    @Override
    public void rejectTask(String messageId)
    {
        Objects.requireNonNull(messageId);
        LOG.debug("Generating reject event for task {}", messageId);
        consumerQueue.add(new ConsumerRejectEvent(Long.parseLong(messageId)));
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
        for ( String consumerTag : consumerTags ) {
            try {
                incomingChannel.basicCancel(consumerTag);
            } catch (IOException e) {
                metrics.incremementErrors();
                LOG.warn("Failed to cancel consumer {}", consumerTag, e);
            }
        }
    }


    @Override
    public void shutdown()
    {
        LOG.debug("Shutting down");
        try {
            if ( publisher != null ) {
                publisher.shutdown();
            }
            if ( consumer != null ) {
                consumer.shutdown();
            }
            if ( conn != null ) {
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
        if ( !conn.isOpen() ) {
            return new HealthResult(HealthStatus.UNHEALTHY, "Rabbit connection failed");
        } else if ( !incomingChannel.isOpen() ) {
            return new HealthResult(HealthStatus.UNHEALTHY, "Incoming channel failed");
        } else if ( !outgoingChannel.isOpen() ) {
            return new HealthResult(HealthStatus.UNHEALTHY, "Outgoing channel failed");
        } else {
            return HealthResult.RESULT_HEALTHY;
        }
    }


    private void createConnection(TaskCallback callback)
        throws IOException, TimeoutException
    {
        RabbitConfiguration rc = config.getRabbitConfiguration();
        ConnectionOptions lyraOpts = RabbitUtil.createLyraConnectionOptions(rc.getRabbitHost(), rc.getRabbitPort(), rc.getRabbitUser(), rc.getRabbitPassword());
        Config lyraConfig = RabbitUtil.createLyraConfig(rc.getBackoffInterval(), rc.getMaxBackoffInterval(), rc.getMaxAttempts());
        lyraConfig.withConnectionListeners(new WorkerConnectionListener(callback));
        conn = RabbitUtil.createRabbitConnection(lyraOpts, lyraConfig);
    }


    private void declareWorkerQueue(Channel channel, String queueName)
        throws IOException
    {
        if ( !declaredQueues.contains(queueName) ) {
            RabbitUtil.declareWorkerQueue(channel, queueName);
        }
    }
}
