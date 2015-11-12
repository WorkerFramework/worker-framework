package com.hpe.caf.worker.queue.rabbit;


import com.hpe.caf.util.rabbitmq.ConsumerAckEvent;
import com.hpe.caf.util.rabbitmq.ConsumerRejectEvent;
import com.hpe.caf.util.rabbitmq.Event;
import com.hpe.caf.util.rabbitmq.QueueConsumer;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.stream.Collectors;


/**
 * A RabbitMQ publisher that will also acknowledge receipt of another message upon
 * publishing of the requested new message. If publishing or acknowledgement fails,
 * the original message that was going to be acknowledged will be rejected.
 * @since 7.5
 */
public class WorkerPublisherImpl implements WorkerPublisher
{
    private final Channel channel;
    private final RabbitMetricsReporter metrics;
    private final BlockingQueue<Event<QueueConsumer>> consumerEvents;
    private static final Logger LOG = LoggerFactory.getLogger(WorkerPublisherImpl.class);


    public WorkerPublisherImpl(Channel ch, RabbitMetricsReporter metrics, BlockingQueue<Event<QueueConsumer>> events)
    {
        this.channel = Objects.requireNonNull(ch);
        this.metrics = Objects.requireNonNull(metrics);
        this.consumerEvents = Objects.requireNonNull(events);
    }


    @Override
    public void handlePublish(byte[] data, String routingKey, long ackId, Map<String, String> headers)
    {
        try {
            LOG.debug("Publishing message with ack id {}", ackId);
            AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties().builder();

            builder.headers(headers.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            builder.contentType("text/plain");
            builder.deliveryMode(2);
            builder.priority(0);
            channel.basicPublish("", routingKey, builder.build(), data);
            metrics.incrementPublished();
            consumerEvents.add(new ConsumerAckEvent(ackId));
        } catch (IOException e) {
            LOG.error("Failed to publish result of message {} to queue {}, rejecting", ackId, routingKey, e);
            metrics.incremementErrors();
            consumerEvents.add(new ConsumerRejectEvent(ackId));
        }
    }
}
