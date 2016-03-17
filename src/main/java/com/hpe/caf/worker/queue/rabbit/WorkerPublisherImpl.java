package com.hpe.caf.worker.queue.rabbit;


import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.DecodeMethod;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.util.rabbitmq.ConsumerRejectEvent;
import com.hpe.caf.util.rabbitmq.Event;
import com.hpe.caf.util.rabbitmq.QueueConsumer;
import com.hpe.caf.util.rabbitmq.RabbitUtil;
import com.hpe.caf.worker.jobtracking.JobTrackingEventType;
import com.hpe.caf.worker.jobtracking.JobTrackingWorkerConstants;
import com.hpe.caf.worker.jobtracking.JobTrackingWorkerTask;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
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
    private final Channel trackingChannel;
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
     * @param trackingChannel the channel to use when publishing tracking messages
     * @param metrics the metrics to report to
     * @param events the event queue of the consumer to ack/reject on
     * @param listener the listener callback that accepts ack/nack publisher confirms from the broker
     * @param inputRoutingKey the routing key on which input messages arrive - tracking messages are not issued if the tracking destination is this key
     * @param codec the codec to use for object serialization/deserialization
     * @throws IOException if the channel cannot have confirmations enabled
     * @since 10.7
     */
    public WorkerPublisherImpl(Channel ch, Channel trackingChannel, RabbitMetricsReporter metrics, BlockingQueue<Event<QueueConsumer>> events, WorkerConfirmListener listener, final String inputRoutingKey, Codec codec)
        throws IOException
    {
        this.channel = Objects.requireNonNull(ch);
        this.trackingChannel = Objects.requireNonNull(trackingChannel);
        this.metrics = Objects.requireNonNull(metrics);
        this.consumerEvents = Objects.requireNonNull(events);
        this.confirmListener = Objects.requireNonNull(listener);
        this.inputRoutingKey = inputRoutingKey;
        this.codec = codec;
        channel.confirmSelect();
        channel.addConfirmListener(confirmListener);
    }


    @Override
    public void handlePublish(byte[] data, String routingKey, long ackId, Map<String, String> headers, JobTrackingEventType trackingEventType)
    {
        try {
            LOG.debug("Publishing message with ack id {}", ackId);
            AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties().builder();
            builder.headers(headers.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
            builder.contentType("text/plain");
            builder.deliveryMode(2);
            builder.priority(0);
            confirmListener.registerResponseSequence(channel.getNextPublishSeqNo(), ackId);
            channel.basicPublish("", routingKey, builder.build(), data);
            metrics.incrementPublished();
            publishTrackingMessage(data, trackingEventType);
        } catch (IOException e) {
            LOG.error("Failed to publish result of message {} to queue {}, rejecting", ackId, routingKey, e);
            metrics.incremementErrors();
            consumerEvents.add(new ConsumerRejectEvent(ackId));
        }
    }


    /**
     * If the raw data is found to be a task message with tracking info, then a new message is published for task tracking purposes
     * @param data if this is a tracked task message, its progress will be reported by the new tracking message published by this method
     * @param trackingEventType describes the type of progress event being reported
     */
    private void publishTrackingMessage(final byte[] data, final JobTrackingEventType trackingEventType) {
        if (trackingEventType == null) {
            return;
        }

        TaskMessage trackedTaskMessage;
        try {
            trackedTaskMessage = codec.deserialise(data, TaskMessage.class, DecodeMethod.LENIENT);
        } catch (CodecException e) {
            LOG.error("Cannot obtain a tracked task message from published data due to error: {}", e);
            return;
        }

        TrackingInfo tracking = trackedTaskMessage.getTracking();
        if (tracking == null) {
            LOG.error("Cannot obtain a tracked task message from published data - task {} has no tracking info", trackedTaskMessage.getTaskId());
            return;
        }

        String trackingPipe = tracking.getTrackingPipe();
        if (trackingPipe == null) {
            LOG.error("Cannot obtain a tracked task message from published data - task {} has no tracking pipe specified in its tracking info", trackedTaskMessage.getTaskId());
            return;
        }

        if (trackingPipe.equalsIgnoreCase(inputRoutingKey)) {
            LOG.error("Skipping creation of a tracking message for task {} - the tracked task message has arrived at the tracking destination", trackedTaskMessage.getTaskId());
            return;
        }

        byte[] trackingMessageData = createTrackingMessageData(trackedTaskMessage.getTaskId(), trackingPipe, trackingEventType);

        try {
            LOG.debug("Publishing tracking message to tracking key {}", trackedTaskMessage.getTracking().getTrackingPipe());
            RabbitUtil.declareWorkerQueue(trackingChannel, trackingPipe);
            AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties().builder();
            builder.headers(Collections.emptyMap());
            builder.contentType("text/plain");
            builder.deliveryMode(2);
            builder.priority(0);
            trackingChannel.basicPublish("", trackingPipe, builder.build(), trackingMessageData);
            metrics.incrementPublished();
        } catch (IOException e) {
            LOG.error("Failed to publish tracking message to queue {}, rejecting", trackingPipe, e);
            metrics.incremementErrors();
        }
    }


    /**
     * Returns a serialised tracking message for the specified task.
     * @param taskId the progress of this task will be reported by the new tracking message returned by this method
     * @param trackingKey the tracking queue to which the tracking message will be published
     * @param eventType describes the type of progress event being reported
     * @return serialised tracking TaskMessage
     */
    private byte[] createTrackingMessageData(final String taskId, final String trackingKey, JobTrackingEventType eventType) {
        try {
            LOG.debug("Creating {} tracking event message for task {})", eventType, taskId);
            JobTrackingWorkerTask eventTask = new JobTrackingWorkerTask();
            eventTask.setTrackedTaskId(taskId);
            eventTask.setEventType(eventType);
            byte[] eventTaskBytes = codec.serialise(eventTask);
            TaskMessage eventMessage =
                    new TaskMessage(taskId,
                            JobTrackingWorkerConstants.WORKER_NAME,
                            JobTrackingWorkerConstants.WORKER_API_VER,
                            eventTaskBytes,
                            TaskStatus.NEW_TASK,
                            Collections.emptyMap(),
                            trackingKey);
            return codec.serialise(eventMessage);
        } catch (CodecException e) {
            LOG.error("Cannot create {} tracking event message for task {}, due to error: {}", eventType, taskId, e);
        }

        return null;
    }
}
