package com.hp.caf.worker.queue.rabbit;


import com.hp.caf.api.worker.NewTaskCallback;
import com.hp.caf.api.worker.WorkerException;
import com.hp.caf.util.rabbitmq.ConsumerEventType;
import com.hp.caf.util.rabbitmq.ConsumerQueueEvent;
import com.hp.caf.util.rabbitmq.RabbitConsumer;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;


/**
 * This is a RabbitMQ QueueingConsumer that also acts as a Thread to handle various events.
 * It is the owner of the 'incoming' channel, and hence is responsible for deliveries,
 * acknowledgements, and rejections of messages.
 */
public class RabbitWorkerQueueConsumer extends RabbitConsumer<ConsumerQueueEvent> implements Runnable
{
    /**
     * Callback to the worker-core to deliver new tasks.
     */
    private final NewTaskCallback callback;
    private final RabbitMetricsReporter metrics;
    private static final Logger LOG = LoggerFactory.getLogger(RabbitWorkerQueueConsumer.class);


    public RabbitWorkerQueueConsumer(final BlockingQueue<Delivery> q, final BlockingQueue<ConsumerQueueEvent> events, final Channel ch,
                                     final NewTaskCallback callback, final RabbitMetricsReporter metrics)
    {
        super(q, events, ch);
        this.callback = Objects.requireNonNull(callback);
        this.metrics = Objects.requireNonNull(metrics);
    }


    @Override
    protected ConsumerQueueEvent getDeliverEvent(final long tag)
    {
        return new ConsumerQueueEvent(ConsumerEventType.DELIVER, tag);
    }


    /**
     * Process a DELIVER event. This involves calling back to worker-core to deliver the task, and in case of an exception,
     * either reject the message for redelivery or dump it on the dead letters exchange.
     */
    @Override
    protected void processDelivery(final Delivery delivery, final ConsumerQueueEvent event)
    {
        if ( delivery != null ) {
            try {
                metrics.incrementReceived();
                LOG.debug("Registering new message {}", delivery.getEnvelope().getDeliveryTag());
                callback.registerNewTask(String.valueOf(delivery.getEnvelope().getDeliveryTag()), delivery.getBody());
            } catch (WorkerException e) {
                LOG.error("Cannot register new message, rejecting {}", delivery.getEnvelope().getDeliveryTag(), e);
                if ( delivery.getEnvelope().isRedeliver() ) {
                    getConsumerEvents().add(new ConsumerQueueEvent(ConsumerEventType.DROP, delivery.getEnvelope().getDeliveryTag()));
                } else {
                    getConsumerEvents().add(new ConsumerQueueEvent(ConsumerEventType.REJECT, delivery.getEnvelope().getDeliveryTag()));
                }
            }
        }
    }


    /**
     * {@inheritDoc}
     *
     * Lyra should handle communication failures (up to the retry limit) but if it still fails,
     * we will requeue the event to reattempt it. If we're in this state, our connection to RabbitMQ is probably dead
     * and the whole microservice is probably unhealthy. If the app is killed here, all it will mean is that the ACK
     * failed to get through and some other worker will take on the responsibility of the task - this is duplicated work,
     * but at least it's not a dropped task.
     */
    @Override
    protected void processAck(final ConsumerQueueEvent event)
    {
        try {
            LOG.debug("Acknowledging message {}", event.getMessageTag());
            getChannel().basicAck(event.getMessageTag(), false);
        } catch (IOException e) {
            LOG.warn("Couldn't ack message {}, will retry", e);
            metrics.incremementErrors();
            getConsumerEvents().add(new ConsumerQueueEvent(ConsumerEventType.ACK, event.getMessageTag()));
        }
    }


    @Override
    protected void processReject(final ConsumerQueueEvent event)
    {
        processReject(event.getMessageTag(), true);
    }


    @Override
    protected void processDrop(final ConsumerQueueEvent event)
    {
        processReject(event.getMessageTag(), false);
    }


    /**
     * Process a REJECT event. Similar to ACK, we will requeue the event if it fails, though Lyra should handle most
     * of our failure cases.
     * @param id the id of the message to reject
     * @param requeue whether to put this message back on the queue or drop it to the dead letters exchange
     */
    private void processReject(final long id, final boolean requeue)
    {
        try {
            getChannel().basicReject(id, requeue);
            if ( requeue ) {
                LOG.debug("Rejecting message {}", id);
                metrics.incrementRejected();
            } else {
                LOG.warn("Dropping message {}", id);
                metrics.incrementDropped();
            }
        } catch (IOException e) {
            LOG.warn("Couldn't reject message {}, will retry", e);
            metrics.incremementErrors();
            getConsumerEvents().add(new ConsumerQueueEvent(requeue ? ConsumerEventType.REJECT : ConsumerEventType.DROP, id));
        }
    }
}
