package com.hp.caf.worker.queue.rabbit;


import com.hp.caf.util.rabbitmq.ConsumerEventType;
import com.hp.caf.util.rabbitmq.ConsumerQueueEvent;
import com.hp.caf.util.rabbitmq.PublishQueueEvent;
import com.hp.caf.util.rabbitmq.RabbitPublisher;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;


/**
 * This is the owner of the 'outgoing' channel, and hence is responsible for publishing results.
 * If it fails to publish a result, it will signal the consumer to reject the original delivery.
 */
public class RabbitWorkerQueuePublisher extends RabbitPublisher<PublishQueueEvent>
{
    private final BlockingQueue<ConsumerQueueEvent> consumerEvents;
    private final RabbitMetricsReporter metrics;
    private static final Logger LOG = LoggerFactory.getLogger(RabbitWorkerQueuePublisher.class);


    public RabbitWorkerQueuePublisher(final BlockingQueue<PublishQueueEvent> publishEvents, final BlockingQueue<ConsumerQueueEvent> consumerEvents,
                                      final Channel outgoingChannel, final RabbitMetricsReporter rabbitMetrics)
    {
        super(publishEvents, outgoingChannel);
        this.consumerEvents = Objects.requireNonNull(consumerEvents);
        this.metrics = Objects.requireNonNull(rabbitMetrics);
    }


    @Override
    protected void handlePublish(final PublishQueueEvent event)
    {
        try {
            LOG.debug("Publishing result for message id {}", event.getMessageTag());
            getChannel().basicPublish("", event.getQueue(), MessageProperties.PERSISTENT_TEXT_PLAIN, event.getEventData());
            metrics.incrementPublished();
            consumerEvents.add(new ConsumerQueueEvent(ConsumerEventType.ACK, event.getMessageTag()));
        } catch (IOException e) {
            LOG.error("Failed to publish result of message {} to queue {}, rejecting", event.getMessageTag(), event.getQueue(), e);
            metrics.incremementErrors();
            consumerEvents.add(new ConsumerQueueEvent(ConsumerEventType.REJECT, event.getMessageTag()));
        }
    }
}
