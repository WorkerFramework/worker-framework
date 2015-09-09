package com.hpe.caf.worker.queue.rabbit;


import com.hpe.caf.util.rabbitmq.ConsumerAckEvent;
import com.hpe.caf.util.rabbitmq.ConsumerRejectEvent;
import com.hpe.caf.util.rabbitmq.Event;
import com.hpe.caf.util.rabbitmq.QueueConsumer;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;


/**
 * A RabbitMQ publisher that will also acknowledge receipt of another message upon
 * publishing of the requested new message. If publishing or acknowledgement fails,
 * the original message that was going to be acknowledged will be rejected.
 */
public class WorkerPublisherImpl implements WorkerPublisher
{
    private final Channel channel;
    private final RabbitMetricsReporter metrics;
    private final BlockingQueue<Event<QueueConsumer>> consumerEvents;
    private static final Logger LOG = LoggerFactory.getLogger(WorkerPublisherImpl.class);


    public WorkerPublisherImpl(final Channel ch, final RabbitMetricsReporter metrics, final BlockingQueue<Event<QueueConsumer>> events)
    {
        this.channel = Objects.requireNonNull(ch);
        this.metrics = Objects.requireNonNull(metrics);
        this.consumerEvents = Objects.requireNonNull(events);
    }


    @Override
    public void handlePublish(final byte[] data, final String queueName, final long ackId)
    {
        try {
            LOG.debug("Publishing result for message id {}", ackId);
            channel.basicPublish("", queueName, MessageProperties.PERSISTENT_TEXT_PLAIN, data);
            metrics.incrementPublished();
            consumerEvents.add(new ConsumerAckEvent(ackId));
        } catch (IOException e) {
            LOG.error("Failed to publish result of message {} to queue {}, rejecting", ackId, queueName, e);
            metrics.incremementErrors();
            consumerEvents.add(new ConsumerRejectEvent(ackId));
        }
    }
}
