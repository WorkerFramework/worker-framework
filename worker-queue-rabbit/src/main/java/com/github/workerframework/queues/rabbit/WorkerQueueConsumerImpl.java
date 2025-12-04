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
import com.github.workerframework.api.DataStoreException;
import com.github.workerframework.api.InvalidTaskException;
import com.github.workerframework.api.ManagedDataStore;
import com.github.workerframework.api.ReferenceNotFoundException;
import com.github.workerframework.api.TaskCallback;
import com.github.workerframework.api.TaskMessage;
import com.github.workerframework.api.TaskRejectedException;
import com.github.workerframework.api.TaskStatus;
import com.github.workerframework.api.TrackingInfo;
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

        // Determine retry count - log which header is being used
        final boolean hasDeliveryCountHeader = deliveryHeaders.containsKey(RabbitHeaders.RABBIT_HEADER_CAF_DELIVERY_COUNT);
        LOG.info("hasDeliveryCountHeader=" + hasDeliveryCountHeader);
        final boolean hasWorkerRetryHeader = deliveryHeaders.containsKey(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY);
        LOG.info("hasWorkerRetryHeader=" + hasWorkerRetryHeader);
        final Object deliveryCountValue = deliveryHeaders.get(RabbitHeaders.RABBIT_HEADER_CAF_DELIVERY_COUNT);
        final Object workerRetryValue = deliveryHeaders.get(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY);

        LOG.info("RETRY CALCULATION START - messageId={}, hasDeliveryCountHeader={}, hasWorkerRetryHeader={}, " +
                     "deliveryCountValue={} (type={}), workerRetryValue={} (type={})",
                 inboundMessageId, hasDeliveryCountHeader, hasWorkerRetryHeader,
                 deliveryCountValue, (deliveryCountValue != null ? deliveryCountValue.getClass().getSimpleName() : "null"),
                 workerRetryValue, (workerRetryValue != null ? workerRetryValue.getClass().getSimpleName() : "null"));

        final int retries;
        if (hasDeliveryCountHeader) {
            final String deliveryCountStr = String.valueOf(deliveryHeaders.getOrDefault(RabbitHeaders.RABBIT_HEADER_CAF_DELIVERY_COUNT, "0"));
            retries = Integer.parseInt(deliveryCountStr);
            LOG.info("RETRY CALCULATION - Using DELIVERY_COUNT header: messageId={}, rawValue={}, stringValue='{}', parsedRetries={}, " +
                         "reason='Quorum queue - delivery count header present'",
                     inboundMessageId, deliveryCountValue, deliveryCountStr, retries);
        } else {
            final String workerRetryStr = String.valueOf(deliveryHeaders.getOrDefault(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY, "0"));
            retries = Integer.parseInt(workerRetryStr);
            LOG.info("RETRY CALCULATION - Using WORKER_RETRY header: messageId={}, rawValue={}, stringValue='{}', parsedRetries={}, " +
                         "reason='Classic queue - no delivery count header, using worker retry header'",
                     inboundMessageId, workerRetryValue, workerRetryStr, retries);
        }



        final Optional<String> taskMessageStorageRefOpt
            = Optional.ofNullable(deliveryHeaders.get(RABBIT_HEADER_CAF_PAYLOAD_OFFLOADING_STORAGE_REF)).map(Object::toString);

        LOG.info("Processing delivery: messageId={}, routingKey={}, isRedelivered={}, retries={}, retryLimit={}, " +
                     "hasDeliveryCountHeader={}, hasRetryHeader={}, deliveryCountValue={}, retryHeaderValue={}, " +
                     "hasStorageRef={}, storageRef={}, allHeaders={}",
                 inboundMessageId, routingKey, isRedelivered, retries, retryLimit,
                 deliveryHeaders.containsKey(RabbitHeaders.RABBIT_HEADER_CAF_DELIVERY_COUNT),
                 deliveryHeaders.containsKey(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY),
                 deliveryHeaders.get(RabbitHeaders.RABBIT_HEADER_CAF_DELIVERY_COUNT),
                 deliveryHeaders.get(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY),
                 taskMessageStorageRefOpt.isPresent(), taskMessageStorageRefOpt.orElse("none"),
                 deliveryHeaders);

        metrics.incrementReceived();

        final byte[] deliveryMessageData = delivery.getMessageData();
        final TaskMessage taskMessage;
        try {
            try {
                LOG.info("Attempting to deserialize TaskMessage for messageId={}", inboundMessageId);
                taskMessage = codec.deserialise(deliveryMessageData, TaskMessage.class, DecodeMethod.LENIENT);
                LOG.info("Successfully deserialized TaskMessage for messageId={}, taskId={}, taskClassifier={}",
                         inboundMessageId, taskMessage.getTaskId(), taskMessage.getTaskClassifier());
            } catch (final CodecException e) {
                LOG.error("Failed to deserialize TaskMessage for messageId={}", inboundMessageId, e);
                handleInvalidDelivery(inboundMessageId, deliveryMessageData, deliveryHeaders,
                                      "Cannot deserialize delivery messageData to TaskMessage");
                return;
            }

            try {
                LOG.info("Handling taskData injection for messageId={}, hasStorageRef={}",
                         inboundMessageId, taskMessageStorageRefOpt.isPresent());
                handleTaskDataInjection(taskMessage, inboundMessageId, taskMessageStorageRefOpt);
                LOG.info("Successfully handled taskData injection for messageId={}", inboundMessageId);
            } catch (final InvalidDeliveryException ex) {
                LOG.error("Invalid delivery detected for messageId={}: {}", inboundMessageId, ex.getMessage(), ex);
                handleMisingOffloadedPayload(inboundMessageId, taskMessage, deliveryMessageData, deliveryHeaders,
                                             ex.getMessage());
                return;
            }

            final PoisonMessageStatus poisonMessageStatus = getPoisonMessageStatus(
                isRedelivered, deliveryHeaders, retries);

            LOG.info("Determined poison message status for messageId={}: status={}, isRedelivered={}, retries={}, retryLimit={}",
                     inboundMessageId, poisonMessageStatus, isRedelivered, retries, retryLimit);

            if (poisonMessageStatus == PoisonMessageStatus.CLASSIC_POSSIBLY_POISON) {
                LOG.info("Message is CLASSIC_POSSIBLY_POISON, republishing to retry queue: messageId={}, " +
                             "routingKey={}, retries={}, retryLimit={}",
                         inboundMessageId, delivery.getEnvelope().getRoutingKey(), retries, retryLimit);
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

            LOG.info("Processing delivery normally: messageId={}, isPoison={}",
                     inboundMessageId, poisonMessageStatus == PoisonMessageStatus.POISON);
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
            LOG.info("Removing offloaded payload from deletion list: messageId={}, hadPayload={}",
                     inboundMessageId, offloadedPayloadsToDelete.containsKey(inboundMessageId));
            offloadedPayloadsToDelete.remove(inboundMessageId);
            //Disconnect the channel to allow for a reconnect when the HealthCheck passes.
            LOG.info("Triggering disconnect callback due to transient error: messageId={}", inboundMessageId);
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

        LOG.info("Checking taskData injection requirements: messageId={}, hasTaskData={}, hasStorageRef={}, storageRef={}",
                 inboundMessageId, hasTaskData, hasStorageRef, taskMessageStorageRefOpt.orElse("none"));

        if (hasTaskData && hasStorageRef) {
            LOG.error("INVALID: Message has both taskData and storage reference: messageId={}, storageRef={}",
                      inboundMessageId, taskMessageStorageRefOpt.get());
            throw new InvalidDeliveryException(
                "TaskMessage contains both taskData and a storage reference. This is invalid.", inboundMessageId);
        }
        if (!hasTaskData && !hasStorageRef) {
            LOG.error("INVALID: Message has neither taskData nor storage reference: messageId={}", inboundMessageId);
            throw new InvalidDeliveryException(
                "TaskMessage contains neither taskData nor a storage reference. This is invalid.", inboundMessageId);
        }
        if (hasStorageRef) {
            LOG.info("Retrieving offloaded taskData from store: messageId={}, storageRef={}",
                     inboundMessageId, taskMessageStorageRefOpt.get());
            final byte[] offloadedTaskData;
            try {
                offloadedTaskData = retrieveTaskDataFromStore(taskMessageStorageRefOpt.get(), inboundMessageId);
                LOG.info("Successfully retrieved offloaded taskData: messageId={}, dataSize={} bytes",
                         inboundMessageId, offloadedTaskData.length);
            } catch (final ReferenceNotFoundException e) {
                LOG.error("Offloaded payload not found: messageId={}, storageRef={}",
                          inboundMessageId, taskMessageStorageRefOpt.get(), e);
                throw new InvalidDeliveryException(e.getMessage(), inboundMessageId);
            }
            taskMessage.setTaskData(offloadedTaskData);
            LOG.info("Injected offloaded taskData into TaskMessage: messageId={}", inboundMessageId);
        } else {
            LOG.info("Using inline taskData (no offloading): messageId={}, dataSize={} bytes",
                     inboundMessageId, currentTaskData.length);
        }
        // If hasTaskData and !hasStorageRef, nothing to do
    }

    private byte[] retrieveTaskDataFromStore(final String taskMessageStorageRef, final long inboundMessageId)
        throws ReferenceNotFoundException, TransientDeliveryException
    {
        LOG.info("Retrieving taskData from datastore: messageId={}, storageRef={}", inboundMessageId, taskMessageStorageRef);
        try (final var inputStream = dataStore.retrieve(taskMessageStorageRef)) {
            final var taskData = inputStream.readAllBytes();
            offloadedPayloadsToDelete.put(inboundMessageId, taskMessageStorageRef);
            LOG.info("Successfully retrieved and scheduled for deletion: messageId={}, storageRef={}, dataSize={} bytes",
                     inboundMessageId, taskMessageStorageRef, taskData.length);
            return taskData;
        } catch (final ReferenceNotFoundException ex) {
            LOG.error("Storage reference not found: messageId={}, storageRef={}", inboundMessageId, taskMessageStorageRef, ex);
            throw ex;
        } catch (final IOException | DataStoreException ex) {
            LOG.error("Transient error retrieving from datastore: messageId={}, storageRef={}",
                      inboundMessageId, taskMessageStorageRef, ex);
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
        LOG.info("Handling invalid delivery: messageId={}, reason={}, invalidRoutingKey={}",
                 inboundMessageId, exceptionMesssage, invalidRoutingKey);

        final RabbitTaskInformation taskInformation = new RabbitTaskInformation(String.valueOf(inboundMessageId), true);
        taskInformation.incrementResponseCount(true);
        final var publishHeaders = new HashMap<>(deliveryHeaders);
        publishHeaders.put(RABBIT_HEADER_CAF_WORKER_INVALID, exceptionMesssage);

        LOG.info("Publishing invalid message: messageId={}, routingKey={}, headerAdded={}",
                 inboundMessageId, invalidRoutingKey, RABBIT_HEADER_CAF_WORKER_INVALID);
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
        LOG.info("Handling missing offloaded payload: messageId={}, reason={}, queue={}",
                 inboundMessageId, exceptionMessage, missingOffloadedPayloadQueue);

        try {
            final RabbitTaskInformation taskInformation = new RabbitTaskInformation(String.valueOf(inboundMessageId), true);
            taskInformation.incrementResponseCount(true);
            final var publishHeaders = new HashMap<>(deliveryHeaders);
            publishHeaders.put(RABBIT_HEADER_CAF_PAYLOAD_OFFLOADING_MISSING, exceptionMessage);

            LOG.info("Publishing to missing offloaded payload queue: messageId={}, queue={}, headerAdded={}",
                     inboundMessageId, missingOffloadedPayloadQueue, RABBIT_HEADER_CAF_PAYLOAD_OFFLOADING_MISSING);
            publisherEventQueue.add(new WorkerPublishQueueEvent(deliveryMessageData, missingOffloadedPayloadQueue, taskInformation, publishHeaders));

            if(taskMessage.getTracking() != null) {
                LOG.info("Sending failure tracking report: messageId={}, trackingPipe={}, jobTaskId={}",
                         inboundMessageId, taskMessage.getTracking().getTrackingPipe(),
                         taskMessage.getTracking().getJobTaskId());
                sendFailureTrackingReport(taskMessage, exceptionMessage, taskInformation);
            } else {
                LOG.info("No tracking info, skipping failure report: messageId={}", inboundMessageId);
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
        LOG.info("Evaluating poison message status: isRedelivered={}, retries={}, retryLimit={}, " +
                     "hasDeliveryCountHeader={}, hasRetryHeader={}",
                 isRedelivered, retries, retryLimit,
                 deliveryHeaders.containsKey(RabbitHeaders.RABBIT_HEADER_CAF_DELIVERY_COUNT),
                 deliveryHeaders.containsKey(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY));

        // If the message is being redelivered it is potentially a poison message.
        if (isRedelivered) {
            LOG.info("Message IS redelivered, checking if classic queue...");
            // If the headers do not contain the delivery count, then it is a classic queue.
            if (!deliveryHeaders.containsKey(RabbitHeaders.RABBIT_HEADER_CAF_DELIVERY_COUNT)) {
                LOG.info("Classic queue detected (no delivery count header). Checking retry limit...");
                // If the retries have not been exceeded, then republish the message
                // with a header recording the retry count
                if (retries < retryLimit) {
                    LOG.info("Retries ({}) < retryLimit ({}), returning CLASSIC_POSSIBLY_POISON", retries, retryLimit);
                    return PoisonMessageStatus.CLASSIC_POSSIBLY_POISON;
                }
                LOG.info("Retries ({}) >= retryLimit ({}), will check final poison status", retries, retryLimit);
            } else {
                LOG.info("Quorum queue detected (has delivery count header)");
            }

            final PoisonMessageStatus status = (retries >= retryLimit)
                ? PoisonMessageStatus.POISON
                : PoisonMessageStatus.NOT_POISON;
            LOG.info("Final redelivered message status: {} (retries={}, retryLimit={})", status, retries, retryLimit);
            return status;
        }

        LOG.info("Message is NOT redelivered, returning NOT_POISON");
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

        LOG.info("Processing delivery with poison status: messageId={}, routingKey={}, isPoison={}, " +
                     "trackingJobTaskId={}, taskId={}",
                 inboundMessageId, routingKey, isPoison, trackingJobTaskId, taskMessage.getTaskId());

        final RabbitTaskInformation taskInformation = new RabbitTaskInformation(
            String.valueOf(inboundMessageId), isPoison, Optional.of(trackingJobTaskId)
        );
        try {
            LOG.debug("Registering new message {}", inboundMessageId);
            LOG.info("Calling callback.registerNewTask for messageId={}", inboundMessageId);
            callback.registerNewTask(taskInformation, taskMessage, deliveryHeaders);
            LOG.info("Successfully registered new task for messageId={}", inboundMessageId);
        } catch (final InvalidTaskException e) {
            LOG.error("Cannot register new message, rejecting {}", inboundMessageId, e);
            LOG.info("Publishing to invalid queue: messageId={}, routingKey={}", inboundMessageId, invalidRoutingKey);
            taskInformation.incrementResponseCount(true);
            final var publishHeaders = new HashMap<String, Object>();
            publishHeaders.put(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_INVALID, e);
            publisherEventQueue.add(new WorkerPublishQueueEvent(taskMessageByteArray, invalidRoutingKey, taskInformation, publishHeaders));
        } catch (final TaskRejectedException e) {
            LOG.warn("Message {} rejected as a task at this time, returning to queue", inboundMessageId, e);
            LOG.info("Republishing rejected task: messageId={}, routingKey={}", inboundMessageId, routingKey);
            taskInformation.incrementResponseCount(true);
            publisherEventQueue.add(new WorkerPublishQueueEvent(taskMessageByteArray, routingKey, taskInformation, deliveryHeaders));
        }
    }

    @Override
    public void processAck(long tag)
    {
        if (tag == -1) {
            LOG.info("Skipping ack for invalid tag: -1");
            return;
        }

        LOG.info("Processing ACK: messageId={}", tag);

        try {
            LOG.debug("Acknowledging message {}", tag);
            channel.basicAck(tag, false);
            LOG.info("Successfully acknowledged message: messageId={}", tag);
        } catch (IOException e) {
            LOG.warn("Couldn't ack message {}, will retry", tag, e);
            metrics.incremementErrors();
            consumerEventQueue.add(new ConsumerAckEvent(tag));
            return;
        }

        final String datastorePayloadReference = offloadedPayloadsToDelete.remove(tag);
        if (datastorePayloadReference != null) {
            LOG.info("Deleting offloaded payload: messageId={}, storageRef={}", tag, datastorePayloadReference);
            try {
                dataStore.delete(datastorePayloadReference, true);
                LOG.info("Successfully deleted offloaded payload: messageId={}, storageRef={}",
                         tag, datastorePayloadReference);
            } catch (final DataStoreException e) {
                LOG.warn("Couldn't delete offloaded payload '{}' for delivery tag '{}' from datastore message.",
                         datastorePayloadReference, tag, e);
            }
        } else {
            LOG.info("No offloaded payload to delete for messageId={}", tag);
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

        LOG.info("Processing REJECT: messageId={}, requeue={}", id, requeue);

        try {
            channel.basicReject(id, requeue);
            if (requeue) {
                LOG.debug("Rejecting message {}", id);
                LOG.info("Message rejected and requeued: messageId={}", id);
                metrics.incrementRejected();
            } else {
                LOG.warn("Dropping message {}", id);
                LOG.info("Message dropped (sent to DLX): messageId={}", id);
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

        LOG.info("Republishing classic redelivery: messageId={}, deliveryQueue={}, retryRoutingKey={}, " +
                     "currentRetries={}, newRetries={}, retryLimit={}, hasStorageRef={}, trackingJobTaskId={}",
                 inboundMessageId, deliveryQueue, retryRoutingKey, retries, retries + 1,
                 retryLimit, taskMessageStorageRefOpt.isPresent(), trackingJobTaskId);

        LOG.debug("Received redelivered message with id {}, retry count {}, retry limit {}, republishing to retry queue",
                  inboundMessageId, retryLimit, retries + 1);

        final Map<String, Object> publishHeaders = new HashMap<>(deliveryHeaders);
        publishHeaders.put(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY, String.valueOf(retries + 1));

        LOG.info("Updated retry header: messageId={}, {}={}",
                 inboundMessageId, RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY, retries + 1);

        taskInformation.incrementResponseCount(true);

        if(taskMessageStorageRefOpt.isPresent()) {
            LOG.info("Message has offloaded payload: messageId={}, storageRef={}",
                     inboundMessageId, taskMessageStorageRefOpt.get());

            if (!retryRoutingKey.equals(deliveryQueue)) {
                LOG.info("Different routing key detected, relocating payload: messageId={}, from={}, to={}",
                         inboundMessageId, deliveryQueue, retryRoutingKey);
                try {
                    final String newStorageReference =
                        dataStore.store(serializedTaskData,
                                        taskMessageStorageRefOpt.get().replace(deliveryQueue, retryRoutingKey));
                    publishHeaders.put(RABBIT_HEADER_CAF_PAYLOAD_OFFLOADING_STORAGE_REF, newStorageReference);
                    LOG.info("Successfully relocated payload: messageId={}, oldRef={}, newRef={}",
                             inboundMessageId, taskMessageStorageRefOpt.get(), newStorageReference);
                }
                catch (final DataStoreException e) {
                    LOG.error("Failed to relocate offloaded payload for message id {} from {} to {}",
                              inboundMessageId, deliveryQueue, retryRoutingKey, e);
                    //Disconnect the channel to allow for a reconnect when the HealthCheck passes.
                    disconnectCallback.run();
                    return;
                }
            }
            else {
                LOG.info("Same routing key, reusing existing payload: messageId={}, routingKey={}, storageRef={}",
                         inboundMessageId, retryRoutingKey, taskMessageStorageRefOpt.get());
                //We are reusing the same routing key, so we do not need to relocate the payload.
                offloadedPayloadsToDelete.remove(inboundMessageId);
            }
        } else {
            LOG.info("Message has inline payload (no offloaded storage): messageId={}", inboundMessageId);
        }

        LOG.info("Adding message to publisher queue: messageId={}, routingKey={}, updatedHeaders={}",
                 inboundMessageId, retryRoutingKey, publishHeaders);
        publisherEventQueue.add(new WorkerPublishQueueEvent(serializedTaskMessage, retryRoutingKey, taskInformation, publishHeaders));
        LOG.info("Successfully queued classic redelivery for republishing: messageId={}", inboundMessageId);
    }
}