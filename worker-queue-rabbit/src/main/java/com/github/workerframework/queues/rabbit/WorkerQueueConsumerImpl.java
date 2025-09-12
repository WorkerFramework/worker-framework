/*
 * Copyright 2015-2025 Open Text.
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
package com.github.workerframework.queues.rabbit;

import com.github.cafapi.common.api.Codec;
import com.github.cafapi.common.api.CodecException;
import com.github.cafapi.common.api.DecodeMethod;
import com.github.workerframework.api.*;
import com.github.workerframework.tracking.report.TrackingReport;
import com.github.workerframework.tracking.report.TrackingReportConstants;
import com.github.workerframework.tracking.report.TrackingReportFailure;
import com.github.workerframework.tracking.report.TrackingReportStatus;
import com.github.workerframework.tracking.report.TrackingReportTask;
import com.github.workerframework.util.rabbitmq.QueueConsumer;
import com.github.workerframework.util.rabbitmq.ConsumerAckEvent;
import com.github.workerframework.util.rabbitmq.Event;
import com.github.workerframework.util.rabbitmq.Delivery;
import com.github.workerframework.util.rabbitmq.RabbitHeaders;
import com.github.workerframework.util.rabbitmq.ConsumerRejectEvent;
import com.github.workerframework.util.rabbitmq.ConsumerDropEvent;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;

import static com.github.workerframework.util.rabbitmq.RabbitHeaders.RABBIT_HEADER_CAF_PAYLOAD_OFFLOADING_MISSING;
import static com.github.workerframework.util.rabbitmq.RabbitHeaders.RABBIT_HEADER_CAF_PAYLOAD_OFFLOADING_STORAGE_REF;
import static com.github.workerframework.util.rabbitmq.RabbitHeaders.RABBIT_HEADER_CAF_WORKER_INVALID;

/**
 * QueueConsumer implementation for a WorkerQueue. This QueueConsumer hands off messages to worker-core upon delivery assuming the message
 * is not marked 'redelivered'. Redelivered messages are republished to the retry queue with an incremented retry count. Redelivered
 * messages that have exceeded the retry count are republished to the rejected queue.
 */
public class WorkerQueueConsumerImpl implements QueueConsumer
{
    private final TaskCallback callback;
    private final RabbitMetricsReporter metrics;
    private final BlockingQueue<Event<QueueConsumer>> consumerEventQueue;
    private final BlockingQueue<Event<WorkerPublisher>> publisherEventQueue;
    private final Channel channel;
    private final String retryRoutingKey;
    private final int retryLimit;
    private final String invalidRoutingKey;
    private final String missingOffloadedPayloadQueue;
    private final ManagedDataStore dataStore;
    private final Codec codec;
    private final Runnable disconnectCallback;
    private final SortedMap<Long, String> offloadedPayloadsToDelete;

    private static final Logger LOG = LoggerFactory.getLogger(WorkerQueueConsumerImpl.class);

    private enum PoisonMessageStatus
    {
        NOT_POISON,
        CLASSIC_POSSIBLY_POISON,
        POISON
    }

    public WorkerQueueConsumerImpl(TaskCallback callback, RabbitMetricsReporter metrics,
                                   BlockingQueue<Event<QueueConsumer>> queue, Channel ch,
                                   BlockingQueue<Event<WorkerPublisher>> pubQueue, String retryKey, int retryLimit,
                                   final String invalidKey,
                                   final ManagedDataStore dataStore, final Codec codec,
                                   final Runnable disconnectCallback,
                                   final String missingOffloadedPayloadQueue)
    {
        this.callback = Objects.requireNonNull(callback);
        this.metrics = Objects.requireNonNull(metrics);
        this.consumerEventQueue = Objects.requireNonNull(queue);
        this.channel = Objects.requireNonNull(ch);
        this.publisherEventQueue = Objects.requireNonNull(pubQueue);
        this.retryRoutingKey = Objects.requireNonNull(retryKey);
        this.retryLimit = retryLimit;
        this.invalidRoutingKey = Objects.requireNonNull(invalidKey);
        this.missingOffloadedPayloadQueue = Objects.requireNonNull(missingOffloadedPayloadQueue);
        this.dataStore = Objects.requireNonNull(dataStore);
        this.codec = Objects.requireNonNull(codec);
        this.disconnectCallback = Objects.requireNonNull(disconnectCallback);
        this.offloadedPayloadsToDelete = Collections.synchronizedSortedMap(new TreeMap<>());
    }

    /**
     * {@inheritDoc}
     * <p>
     * If an incoming message is marked as redelivered, hand it off to another method to deal with retry/rejection. Otherwise, hand it off
     * to worker-core, and potentially republish or reject it depending upon exceptions thrown.
     */
    @Override
    public void processDelivery(Delivery delivery)
    {
        final long inboundMessageId = delivery.getEnvelope().getDeliveryTag();
        final String routingKey = delivery.getEnvelope().getRoutingKey();
        final Map<String, Object> deliveryHeaders = delivery.getHeaders();
        final boolean isRedelivered = delivery.getEnvelope().isRedeliver();
        final int retries = deliveryHeaders.containsKey(RabbitHeaders.RABBIT_HEADER_CAF_DELIVERY_COUNT)
            ? Integer.parseInt(String.valueOf(deliveryHeaders.getOrDefault(RabbitHeaders.RABBIT_HEADER_CAF_DELIVERY_COUNT, "0")))
            : Integer.parseInt(String.valueOf(deliveryHeaders.getOrDefault(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY, "0")));
        final Optional<String> taskMessageStorageRefOpt
            = Optional.ofNullable(deliveryHeaders.get(RABBIT_HEADER_CAF_PAYLOAD_OFFLOADING_STORAGE_REF)).map(Object::toString);
        metrics.incrementReceived();

        final byte[] deliveryMessageData = delivery.getMessageData();
        final TaskMessage taskMessage;
        try {
            try {
                taskMessage = codec.deserialise(deliveryMessageData, TaskMessage.class, DecodeMethod.LENIENT);
            } catch (final CodecException e) {
                handleInvalidDelivery(inboundMessageId, deliveryMessageData, deliveryHeaders,
                    "Cannot deserialize delivery messageData to TaskMessage");
                return;
            }

            try {
                handleTaskDataInjection(taskMessage, inboundMessageId, taskMessageStorageRefOpt);
            } catch (final InvalidDeliveryException ex) {
                handleMisingOffloadedPayload(inboundMessageId, taskMessage, deliveryMessageData, deliveryHeaders,
                    ex.getMessage());
                return;
            }

            final PoisonMessageStatus poisonMessageStatus = getPoisonMessageStatus(
                isRedelivered, deliveryHeaders, retries);

            if (poisonMessageStatus == PoisonMessageStatus.CLASSIC_POSSIBLY_POISON) {
                republishClassicRedelivery(
                        delivery.getEnvelope().getRoutingKey(),
                        inboundMessageId,
                        deliveryMessageData,
                        taskMessage.getTaskData(),
                        deliveryHeaders,
                        retries,
                        taskMessage.getTracking(),
                        taskMessageStorageRefOpt
                );
                return;
            }

            processDelivery(
                    inboundMessageId,
                    routingKey,
                    deliveryHeaders,
                    taskMessage,
                    deliveryMessageData,
                    poisonMessageStatus == PoisonMessageStatus.POISON
            );
        } catch (final TransientDeliveryException e) {
            LOG.warn("Transient error processing message id {}, disconnecting.", inboundMessageId, e);
            offloadedPayloadsToDelete.remove(inboundMessageId);
            //Disconnect the channel to allow for a reconnect when the HealthCheck passes.
            disconnectCallback.run();
        }
    }

    /**
     * Handles the logic for injecting taskData from the store into the TaskMessage if required.
     * Returns true if processing should continue, false if it should stop (e.g. error).
     * If invalid, handles as poison message (publishes to retry queue) and returns false.
     */
    private void handleTaskDataInjection(final TaskMessage taskMessage, final long inboundMessageId,
                                         final Optional<String> taskMessageStorageRefOpt) 
        throws InvalidDeliveryException, TransientDeliveryException
    {
        final byte[] currentTaskData = taskMessage.getTaskData();
        final boolean hasStorageRef = taskMessageStorageRefOpt.isPresent();
        final boolean hasTaskData = currentTaskData != null;

        if (hasTaskData && hasStorageRef) {
            throw new InvalidDeliveryException(
                    "TaskMessage contains both taskData and a storage reference. This is invalid.", inboundMessageId);
        }
        if (!hasTaskData && !hasStorageRef) {
            throw new InvalidDeliveryException(
                    "TaskMessage contains neither taskData nor a storage reference. This is invalid.", inboundMessageId);
        }
        if (hasStorageRef) {
            final byte[] offloadedTaskData;
            try {
                offloadedTaskData = retrieveTaskDataFromStore(taskMessageStorageRefOpt.get(), inboundMessageId);
            } catch (final ReferenceNotFoundException e) {
                throw new InvalidDeliveryException(e.getMessage(), inboundMessageId);
            }
            taskMessage.setTaskData(offloadedTaskData);
        }
        // If hasTaskData and !hasStorageRef, nothing to do
    }

    private byte[] retrieveTaskDataFromStore(final String taskMessageStorageRef, final long inboundMessageId) 
            throws ReferenceNotFoundException, TransientDeliveryException
    {
        try (final var inputStream = dataStore.retrieve(taskMessageStorageRef)) {
            final var taskData = inputStream.readAllBytes();
            offloadedPayloadsToDelete.put(inboundMessageId, taskMessageStorageRef);
            return taskData;
        } catch (final ReferenceNotFoundException ex) {
            throw ex;
        } catch (final IOException | DataStoreException ex) {
            throw new TransientDeliveryException(
                "TaskMessage's TaskData could not be retrieved from DataStore", inboundMessageId, ex);
        }
    }

    private void handleInvalidDelivery(
        final long inboundMessageId,
        final byte[] deliveryMessageData,
        final Map<String, Object> deliveryHeaders,
        final String exceptionMesssage
    )
    {
        final RabbitTaskInformation taskInformation = new RabbitTaskInformation(String.valueOf(inboundMessageId), true);
        taskInformation.incrementResponseCount(true);
        final var publishHeaders = new HashMap<>(deliveryHeaders);
        publishHeaders.put(RABBIT_HEADER_CAF_WORKER_INVALID, exceptionMesssage);

        publisherEventQueue.add(new WorkerPublishQueueEvent(deliveryMessageData, invalidRoutingKey, taskInformation, publishHeaders));
    }

    private void handleMisingOffloadedPayload(
            final long inboundMessageId,
            final TaskMessage taskMessage,
            final byte[] deliveryMessageData,
            final Map<String, Object> deliveryHeaders,
            final String exceptionMessage
    )
    {
        try {
            final RabbitTaskInformation taskInformation = new RabbitTaskInformation(String.valueOf(inboundMessageId), true);
            taskInformation.incrementResponseCount(true);
            final var publishHeaders = new HashMap<>(deliveryHeaders);
            publishHeaders.put(RABBIT_HEADER_CAF_PAYLOAD_OFFLOADING_MISSING, exceptionMessage);

            publisherEventQueue.add(new WorkerPublishQueueEvent(deliveryMessageData, missingOffloadedPayloadQueue, taskInformation, publishHeaders));

            if(taskMessage.getTracking() != null) {
                sendFailureTrackingReport(taskMessage, exceptionMessage, taskInformation);
            }
        } catch (final CodecException e) {
            LOG.error("Failed to serialise report update task data.");
            throw new RuntimeException(e);
        }
    }

    private void sendFailureTrackingReport(
        final TaskMessage taskMessage,
        final String invalidDeliveryExceptionMessage,
        final RabbitTaskInformation rabbitTaskInformation
    ) throws CodecException {
        final TrackingReportFailure failure = new TrackingReportFailure();
        failure.failureId = TaskStatus.INVALID_TASK.toString();
        failure.failureTime = new Date();
        failure.failureSource = taskMessage.getTo(); // queue name
        failure.failureMessage = invalidDeliveryExceptionMessage;

        final List<TrackingReport> trackingReports = new ArrayList<>();

        final TrackingInfo trackingInfo = taskMessage.getTracking();

        final TrackingReport trackingReport = new TrackingReport();
        trackingReport.jobTaskId = trackingInfo.getJobTaskId();
        trackingReport.status = TrackingReportStatus.Failed;
        trackingReport.failure = failure;
        trackingReport.estimatedPercentageCompleted = 0;

        trackingReports.add(trackingReport);

        final TrackingReportTask trackingReportTask = new TrackingReportTask();
        trackingReportTask.trackingReports = trackingReports;

        final byte[] trackingReportTaskTaskData = codec.serialise(trackingReportTask);

        final TaskMessage failureReportTaskMessage = new TaskMessage(
            UUID.randomUUID().toString(), TrackingReportConstants.TRACKING_REPORT_TASK_NAME,
            TrackingReportConstants.TRACKING_REPORT_TASK_API_VER, trackingReportTaskTaskData, TaskStatus.NEW_TASK,
            Collections.emptyMap(), trackingInfo.getTrackingPipe(), null, null,
            taskMessage.getCorrelationId());

        publisherEventQueue.add(new WorkerPublishQueueEvent(codec.serialise(failureReportTaskMessage),
            trackingInfo.getTrackingPipe(), rabbitTaskInformation, Collections.emptyMap()));
    }

    private PoisonMessageStatus getPoisonMessageStatus(
        final boolean isRedelivered,
        final Map<String, Object> deliveryHeaders,
        final int retries
    ) {
        // If the message is being redelivered it is potentially a poison message.
        if (isRedelivered) {
            // If the headers do not contain the delivery count, then it is a classic queue.
            if (!deliveryHeaders.containsKey(RabbitHeaders.RABBIT_HEADER_CAF_DELIVERY_COUNT)) {
                // If the retries have not been exceeded, then republish the message
                // with a header recording the retry count
                if (retries < retryLimit) {
                    return PoisonMessageStatus.CLASSIC_POSSIBLY_POISON;
                }
            }
            return (retries >= retryLimit)
                ? PoisonMessageStatus.POISON
                : PoisonMessageStatus.NOT_POISON;
        }
        return PoisonMessageStatus.NOT_POISON;
    }

    private void processDelivery(
        final long inboundMessageId,
        final String routingKey,
        final Map<String, Object> deliveryHeaders,
        final TaskMessage taskMessage,
        final byte[] taskMessageByteArray,
        final boolean isPoison
    ) {
        final TrackingInfo trackingInfo = taskMessage.getTracking();
        final String trackingJobTaskId = trackingInfo != null ? trackingInfo.getJobTaskId() : "untracked";
        final RabbitTaskInformation taskInformation = new RabbitTaskInformation(
            String.valueOf(inboundMessageId), isPoison, Optional.of(trackingJobTaskId)
        );
        try {
            LOG.debug("Registering new message {}", inboundMessageId);
            callback.registerNewTask(taskInformation, taskMessage, deliveryHeaders);
        } catch (final InvalidTaskException e) {
            LOG.error("Cannot register new message, rejecting {}", inboundMessageId, e);
            taskInformation.incrementResponseCount(true);
            final var publishHeaders = new HashMap<String, Object>();
            publishHeaders.put(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_INVALID, e);
            publisherEventQueue.add(new WorkerPublishQueueEvent(taskMessageByteArray, invalidRoutingKey, taskInformation, publishHeaders));
        } catch (final TaskRejectedException e) {
            LOG.warn("Message {} rejected as a task at this time, returning to queue", inboundMessageId, e);
            taskInformation.incrementResponseCount(true);
            publisherEventQueue.add(new WorkerPublishQueueEvent(taskMessageByteArray, routingKey, taskInformation, deliveryHeaders));
        }
    }

    @Override
    public void processAck(long tag)
    {
        if (tag == -1) {
            return;
        }

        try {
            LOG.debug("Acknowledging message {}", tag);
            channel.basicAck(tag, false);
        } catch (IOException e) {
            LOG.warn("Couldn't ack message {}, will retry", tag, e);
            metrics.incremementErrors();
            consumerEventQueue.add(new ConsumerAckEvent(tag));
            return;
        }

        final String datastorePayloadReference = offloadedPayloadsToDelete.remove(tag);
        if (datastorePayloadReference != null) {
            final Path referenceFilePath = getReferenceFilePath(datastorePayloadReference, tag);
            try {
                dataStore.delete(datastorePayloadReference);
            } catch (final DataStoreException e) {
                LOG.warn("Couldn't delete offloaded payload '{}' for delivery tag '{}' from datastore message.",
                         datastorePayloadReference, tag, e);
            }
            if (referenceFilePath != null && dataStore instanceof DirectoryManager) {
                final var directoryManager = (DirectoryManager) dataStore;
                try {
                    directoryManager.deleteDirectory(referenceFilePath.getParent());
                } catch (final DataStoreException e) {
                    LOG.warn("Couldn't delete offloaded payload directory'{}' for delivery tag '{}' from datastore message.",
                            datastorePayloadReference, tag, e);
                }
            }
        }
    }

    @Override
    public void processReject(long tag)
    {
        processReject(tag, true);
    }

    @Override
    public void processDrop(long tag)
    {
        processReject(tag, false);
    }

    /**
     * Process a REJECT event. Similar to ACK, we will requeue the event if it fails, though the RabbitMQ java client should handle most of our failure cases.
     *
     * @param id the id of the message to reject
     * @param requeue whether to put this message back on the queue or drop it to the dead letters exchange
     */
    private void processReject(long id, boolean requeue)
    {
        if (id == -1) {
            LOG.error("Non-final response has not been acknowledged. This message has been lost!");
            return;
        }

        try {
            channel.basicReject(id, requeue);
            if (requeue) {
                LOG.debug("Rejecting message {}", id);
                metrics.incrementRejected();
            } else {
                LOG.warn("Dropping message {}", id);
                metrics.incrementDropped();
            }
        } catch (IOException e) {
            LOG.warn("Couldn't reject message {}, will retry", id, e);
            metrics.incremementErrors();
            consumerEventQueue.add(requeue ? new ConsumerRejectEvent(id) : new ConsumerDropEvent(id));
        }
    }

    private void republishClassicRedelivery(
        final String deliveryQueue,
        final long inboundMessageId,
        final byte[] serializedTaskMessage,
        final byte[] serializedTaskData,
        final  Map<String, Object> deliveryHeaders,
        final int retries,
        final TrackingInfo tracking,
        final Optional<String> taskMessageStorageRefOpt
    )
    {
        final String trackingJobTaskId = tracking != null ? tracking.getJobTaskId() : "untracked";
        final RabbitTaskInformation taskInformation = new RabbitTaskInformation(
            String.valueOf(inboundMessageId), false, Optional.of(trackingJobTaskId));
        LOG.debug("Received redelivered message with id {}, retry count {}, retry limit {}, republishing to retry queue",
                  inboundMessageId, retryLimit, retries + 1);
        final Map<String, Object> publishHeaders = new HashMap<>(deliveryHeaders);
        publishHeaders.put(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY, String.valueOf(retries + 1));
        taskInformation.incrementResponseCount(true);
        if(taskMessageStorageRefOpt.isPresent()) {
            if (!retryRoutingKey.equals(deliveryQueue)) {
                try {
                    final String newStorageReference = 
                            dataStore.store(serializedTaskData,
                                taskMessageStorageRefOpt.get().replace(deliveryQueue, retryRoutingKey));
                    publishHeaders.put(RABBIT_HEADER_CAF_PAYLOAD_OFFLOADING_STORAGE_REF, newStorageReference);
                } 
                catch (final DataStoreException e) {
                    LOG.error("Failed to relocate offloaded payload for message id {} from {} to {}",
                            inboundMessageId, deliveryQueue, retryRoutingKey, e);
                    //Disconnect the channel to allow for a reconnect when the HealthCheck passes.
                    disconnectCallback.run();
                }
            }
            else {
                //We are reusing the same routing key, so we do not need to relocate the payload.
                offloadedPayloadsToDelete.remove(inboundMessageId);
            }
        }
        publisherEventQueue.add(new WorkerPublishQueueEvent(serializedTaskMessage, retryRoutingKey, taskInformation, publishHeaders));
    }

    private Path getReferenceFilePath(final String datastorePayloadReference, final long tag) {
        if (dataStore instanceof FilePathProvider) {
            final FilePathProvider filePathProvider = (FilePathProvider) dataStore;
            try {
                return filePathProvider.getFilePath(datastorePayloadReference);
            } catch (final DataStoreException e) {
                LOG.warn("Couldn't recover offloaded payload filepath '{}' for delivery tag '{}' from datastore message.",
                        datastorePayloadReference, tag, e);
            }
        }
        return null;
    }
}
