package com.hpe.caf.worker.queue.rabbit;


import com.hpe.caf.api.worker.TaskCallback;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.util.rabbitmq.ConsumerAckEvent;
import com.hpe.caf.util.rabbitmq.ConsumerDropEvent;
import com.hpe.caf.util.rabbitmq.ConsumerRejectEvent;
import com.hpe.caf.util.rabbitmq.Delivery;
import com.hpe.caf.util.rabbitmq.Event;
import com.hpe.caf.util.rabbitmq.QueueConsumer;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;


/**
 * QueueConsumer implementation for a WorkerQueue.
 * This QueueConsumer hands off messages to worker-core upon delivery, rejecting or
 * dropping messages if this fails (depending upon if this is the first try or not).
 */
public class WorkerQueueConsumerImpl implements QueueConsumer
{
    private final TaskCallback callback;
    private final RabbitMetricsReporter metrics;
    private final BlockingQueue<Event<QueueConsumer>> consumerEventQueue;
    private final Channel channel;
    private static final Logger LOG = LoggerFactory.getLogger(WorkerQueueConsumerImpl.class);


    public WorkerQueueConsumerImpl(final TaskCallback callback, final RabbitMetricsReporter metrics, final BlockingQueue<Event<QueueConsumer>> queue, final Channel ch)
    {
        this.callback = Objects.requireNonNull(callback);
        this.metrics = Objects.requireNonNull(metrics);
        this.consumerEventQueue = Objects.requireNonNull(queue);
        this.channel = Objects.requireNonNull(ch);
    }


    @Override
    public void processDelivery(final Delivery delivery)
    {
        long tag = delivery.getEnvelope().getDeliveryTag();
        try {
            metrics.incrementReceived();
            LOG.debug("Registering new message {}", tag);
            callback.registerNewTask(String.valueOf(tag), delivery.getMessageData());
        } catch (WorkerException e) {
            LOG.error("Cannot register new message, rejecting {}", tag, e);
            if ( delivery.getEnvelope().isRedeliver() ) {
                consumerEventQueue.add(new ConsumerDropEvent(tag));
            } else {
                consumerEventQueue.add(new ConsumerRejectEvent(tag));
            }
        }
    }


    @Override
    public void processAck(final long tag)
    {
        try {
            LOG.debug("Acknowledging message {}", tag);
            channel.basicAck(tag, false);
        } catch (IOException e) {
            LOG.warn("Couldn't ack message {}, will retry", e);
            metrics.incremementErrors();
            consumerEventQueue.add(new ConsumerAckEvent(tag));
        }
    }


    @Override
    public void processReject(final long tag)
    {
        processReject(tag, true);
    }


    @Override
    public void processDrop(final long tag)
    {
        processReject(tag, false);
    }


    /**
     * Process a REJECT event. Similar to ACK, we will requeue the event if it fails, though Lyra should handle most
     * of our failure cases.
     *
     * @param id the id of the message to reject
     * @param requeue whether to put this message back on the queue or drop it to the dead letters exchange
     */
    private void processReject(final long id, final boolean requeue)
    {
        try {
            channel.basicReject(id, requeue);
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
            consumerEventQueue.add(requeue ? new ConsumerRejectEvent(id) : new ConsumerDropEvent(id));
        }
    }
}
