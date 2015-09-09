package com.hpe.caf.worker.queue.rabbit;


import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;
import com.hpe.caf.api.worker.NewTaskCallback;
import com.hpe.caf.api.worker.QueueException;
import com.hpe.caf.api.worker.WorkerQueue;
import com.hpe.caf.api.worker.WorkerQueueMetricsReporter;
import com.hpe.caf.util.rabbitmq.ConsumerRejectEvent;
import com.hpe.caf.util.rabbitmq.DefaultRabbitConsumer;
import com.hpe.caf.util.rabbitmq.Event;
import com.hpe.caf.util.rabbitmq.EventPoller;
import com.hpe.caf.util.rabbitmq.QueueConsumer;
import com.hpe.caf.util.rabbitmq.RabbitUtil;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
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
 * These threads handle operations via a BlockingQueue of QueueEvent objects. In all scenarios where the
 * tasks triggered by the message take significantly longer than the handling of the messages themselves
 * (which should hopefully be true of all microservices), this implementation should work. The complexity
 * comes from the fact each thread should have its own Channel object (they are not thread-safe) but the
 * thread that received a message should also be the one to acknowledge it.
 */
public final class RabbitWorkerQueue extends WorkerQueue
{
    private DefaultRabbitConsumer consumer;
    private EventPoller<WorkerPublisher> publisher;
    private final String inputQueue;
    private final Channel incomingChannel;
    private final Channel outgoingChannel;
    private final Connection conn;
    private final List<String> consumerTags = new LinkedList<>();
    private final Set<String> declaredQueues = new HashSet<>();
    private final BlockingQueue<Event<QueueConsumer>> consumerQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<Event<WorkerPublisher>> publisherQueue = new LinkedBlockingQueue<>();
    private final RabbitMetricsReporter metrics = new RabbitMetricsReporter();
    private static final Logger LOG = LoggerFactory.getLogger(RabbitWorkerQueue.class);


    /**
     * {@inheritDoc}
     *
     * Create a RabbitMQ connection, and separate incoming and outgoing channels. The connection and channels are managed by Lyra, so
     * will attempt to re-establish should they drop. A dead letter exchange is declared.
     */
    public RabbitWorkerQueue(final RabbitWorkerQueueConfiguration config, final int maxTasks)
        throws QueueException
    {
        super(maxTasks);
        int prefetch = Math.max(1, maxTasks + config.getPrefetchBuffer());
        try {
            conn = RabbitUtil.createRabbitConnection(config.getRabbitConfiguration());
            incomingChannel = conn.createChannel();
            LOG.debug("Prefetch is {}", prefetch);
            incomingChannel.basicQos(prefetch);
            incomingChannel.exchangeDeclare(config.getDeadLetterExchange(), "direct");
            inputQueue = config.getInputQueue();
            outgoingChannel = conn.createChannel();
        } catch (IOException | TimeoutException e) {
            throw new QueueException("Failed to create queue server connection", e);
        }
        LOG.debug("Initialised");
    }


    /**
     * {@inheritDoc}
     *
     * Declare the queues on the appropriate channels and kick off the publisher and consumer threads to handle messages.
     */
    @Override
    public void start(final NewTaskCallback callback)
        throws QueueException
    {
        try {
            WorkerQueueConsumerImpl consumerImpl = new WorkerQueueConsumerImpl(callback, metrics, consumerQueue, incomingChannel);
            consumer = new DefaultRabbitConsumer(consumerQueue, consumerImpl);
            WorkerPublisherImpl publisherImpl = new WorkerPublisherImpl(outgoingChannel, metrics, consumerQueue);
            publisher = new EventPoller<>(2, publisherQueue, publisherImpl);
            declareWorkerQueue(incomingChannel, inputQueue);
            consumerTags.add(incomingChannel.basicConsume(inputQueue, consumer));
        } catch (IOException e) {
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
    public void publish(final String acknowledgeId, final byte[] taskMessage, final String targetQueue)
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
    public void rejectTask(final String messageId)
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
            incomingChannel.close();
            outgoingChannel.close();
            conn.close();
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


    private void declareWorkerQueue(final Channel channel, final String queueName)
        throws IOException
    {
        if ( !declaredQueues.contains(queueName) ) {
            RabbitUtil.declareWorkerQueue(channel, queueName);
        }
    }
}
