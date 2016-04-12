package com.hpe.caf.worker.queue.rabbit;


import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.DecodeMethod;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.util.rabbitmq.ConsumerRejectEvent;
import com.hpe.caf.util.rabbitmq.Event;
import com.hpe.caf.util.rabbitmq.QueueConsumer;
import com.hpe.caf.util.rabbitmq.RabbitUtil;
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
 * A RabbitMQ publisher that uses a ConfirmListener, sending data as plain text with headers.
 * Messages that cannot be published at all cause a rejection of the input message (task) that
 * triggered this published response.
 * @since 7.5
 */
public class WorkerPublisherImpl implements WorkerPublisher
{
    private final Channel channel;
    private final RabbitMetricsReporter metrics;
    private final BlockingQueue<Event<QueueConsumer>> consumerEvents;
    private final WorkerConfirmListener confirmListener;
    private final String inputRoutingKey;
    private final Codec codec;
    private static final Logger LOG = LoggerFactory.getLogger(WorkerPublisherImpl.class);


    /**
     * Create a WorkerPublisher implementation. The channel will have confirmations turned on
     * and the supplied WorkerConfirmListener will be added as a confirm listener upon the channel.
     * @param ch the channel to use, will have confirmations enabled
     * @param metrics the metrics to report to
     * @param events the event queue of the consumer to ack/reject on
     * @param listener the listener callback that accepts ack/nack publisher confirms from the broker
     * @param inputRoutingKey the routing key on which input messages arrive - messages are not diverted for tracking if the tracking destination is this key
     * @param codec the codec to use for object serialization/deserialization
     * @throws IOException if the channel cannot have confirmations enabled
     * @since 10.7
     */
    public WorkerPublisherImpl(Channel ch, RabbitMetricsReporter metrics, BlockingQueue<Event<QueueConsumer>> events, WorkerConfirmListener listener, final String inputRoutingKey, Codec codec)
        throws IOException
    {
        this.channel = Objects.requireNonNull(ch);
        this.metrics = Objects.requireNonNull(metrics);
        this.consumerEvents = Objects.requireNonNull(events);
        this.confirmListener = Objects.requireNonNull(listener);
        this.inputRoutingKey = inputRoutingKey;
        this.codec = codec;
        channel.confirmSelect();
        channel.addConfirmListener(confirmListener);
    }


    @Override
    public void handlePublish(byte[] data, String routingKey, long ackId, Map<String, String> headers)
    {
        String publishKey = routingKey;
        try {
            LOG.debug("Publishing message with ack id {}", ackId);
            AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties().builder();
            builder.headers(headers.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            builder.contentType("text/plain");
            builder.deliveryMode(2);
            builder.priority(0);
            confirmListener.registerResponseSequence(channel.getNextPublishSeqNo(), ackId);
            publishKey = getPublishKey(data, routingKey);
            channel.basicPublish("", publishKey, builder.build(), data);
            metrics.incrementPublished();
        } catch (IOException e) {
            LOG.error("Failed to publish result of message {} to queue {}, rejecting", ackId, publishKey, e);
            metrics.incremementErrors();
            consumerEvents.add(new ConsumerRejectEvent(ackId));
        }
    }


    /**
     * Determines the queue to publish to and ensures that this queue is declared on the channel.
     * If the data is found to be a task message with tracking info then the tracking info should dictate the queue to publish to.
     * @param data checks this data to determine tracking info
     * @param routingKey publish to this queue if no tracking info or tracking destination can be found in the supplied data
     * @return the queue to publish to
     */
    private String getPublishKey(final byte[] data, final String routingKey) {
        TaskMessage trackedTaskMessage;
        try {
            trackedTaskMessage = codec.deserialise(data, TaskMessage.class, DecodeMethod.LENIENT);
        } catch (CodecException e) {
            LOG.error("Publishing to queue {} - cannot obtain a tracked task message from published data due to error: {}",routingKey, e);
            return routingKey;
        }

        TrackingInfo tracking = trackedTaskMessage.getTracking();
        if (tracking == null) {
            LOG.debug("Cannot obtain a tracked task message from published data - task {} has no tracking info so publishing to queue {}", trackedTaskMessage.getTaskId(), routingKey);
            return routingKey;
        }

        String trackingPipe = tracking.getTrackingPipe();
        if (trackingPipe == null) {
            LOG.warn("Cannot obtain a tracked task message from published data - task {} has no tracking pipe specified in its tracking info so publishing to queue {}", trackedTaskMessage.getTaskId(), routingKey);
            return routingKey;
        }

        if (trackingPipe.equalsIgnoreCase(inputRoutingKey)) {
            LOG.debug("Task {} - the tracked task message has arrived at the tracking destination {} so does not need to be diverted to that destination", trackedTaskMessage.getTaskId(), trackingPipe);
            return routingKey;
        }

        try {
            RabbitUtil.declareWorkerQueue(channel, trackingPipe);
        } catch (IOException e) {
            LOG.warn("Task {} - the tracking destination {} could not be declared so publishing to queue {}", trackedTaskMessage.getTaskId(), trackingPipe, routingKey);
            return routingKey;
        }

        return trackingPipe;
    }
}
