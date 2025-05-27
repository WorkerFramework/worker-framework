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
import com.github.workerframework.api.TaskCallback;
import com.github.workerframework.api.TaskMessage;
import com.github.workerframework.api.TaskRejectedException;
import com.github.workerframework.api.TrackingInfo;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;

import static com.github.workerframework.util.rabbitmq.RabbitHeaders.RABBIT_HEADER_CAF_PAYLOAD_OFFLOADING_STORAGE_REF;

/**
 * QueueConsumer implementation for a WorkerQueue. This QueueConsumer hands off messages to worker-core upon delivery assuming the message
 * is not marked 'redelivered'. Redelivered messages are republished to the retry queue with an incremented retry count. Redelivered
 * messages that have exceeded the retry count are republished to the rejected queue.
 */
public class WorkerQueueConsumerImpl implements QueueConsumer
{
    public static final String REJECTED_REASON_TASKMESSAGE = "TASKMESSAGE_INVALID";
    private static final String REJECTED_REASON_PAYLOAD_OFFLOADING_TASKMESSAGE_DATASTORE_ERROR = "TASKMESSAGE_DATASTORE_ERROR";

    private final TaskCallback callback;
    private final RabbitMetricsReporter metrics;
    private final BlockingQueue<Event<QueueConsumer>> consumerEventQueue;
    private final BlockingQueue<Event<WorkerPublisher>> publisherEventQueue;
    private final Channel channel;
    private final String retryRoutingKey;
    private final int retryLimit;
    private final ManagedDataStore dataStore;
    private final Codec codec;
    private final SortedMap<Long, String> offloadedPayloads;

    private static final Logger LOG = LoggerFactory.getLogger(WorkerQueueConsumerImpl.class);

    private enum PoisonMessageStatus
    {
        NOT_POISON,
        CLASSIC_AND_REPUBLISHED,
        POISON
    }

    public WorkerQueueConsumerImpl(TaskCallback callback, RabbitMetricsReporter metrics, BlockingQueue<Event<QueueConsumer>> queue, Channel ch,
                                   BlockingQueue<Event<WorkerPublisher>> pubQueue, String retryKey, int retryLimit,
                                   final ManagedDataStore dataStore, final Codec codec) {
        this.callback = Objects.requireNonNull(callback);
        this.metrics = Objects.requireNonNull(metrics);
        this.consumerEventQueue = Objects.requireNonNull(queue);
        this.channel = Objects.requireNonNull(ch);
        this.publisherEventQueue = Objects.requireNonNull(pubQueue);
        this.retryRoutingKey = Objects.requireNonNull(retryKey);
        this.retryLimit = retryLimit;
        this.dataStore = Objects.requireNonNull(dataStore);
        this.codec = Objects.requireNonNull(codec);
        this.offloadedPayloads = Collections.synchronizedSortedMap(new TreeMap<>());
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

        final byte[] taskMessageData = retrieveTaskMessageData(delivery, taskMessageStorageRefOpt, inboundMessageId);
        if (taskMessageData == null) {
            return;
        }

        final TaskMessage taskMessage = deserializeTaskMessage(taskMessageData, inboundMessageId, taskMessageStorageRefOpt);
        if (taskMessage == null) {
            return;
        }

        final PoisonMessageStatus poisonMessageStatus = getPoisonMessageStatus(
            isRedelivered, inboundMessageId, taskMessageData, deliveryHeaders, retries, taskMessage.getTracking());
        if (poisonMessageStatus == PoisonMessageStatus.CLASSIC_AND_REPUBLISHED) {
            return;
        }

        processDelivery(
            inboundMessageId,
            routingKey,
            deliveryHeaders,
            taskMessage,
            taskMessageData,
            poisonMessageStatus == PoisonMessageStatus.POISON
        );
    }

    private byte[] retrieveTaskMessageData(
        final Delivery delivery,
        final Optional<String> taskMessageStorageRefOpt,
        final long inboundMessageId
    ) {
        if (taskMessageStorageRefOpt.isPresent()) {
            final String taskMessageStorageRef = taskMessageStorageRefOpt.get();  
            offloadedPayloads.put(inboundMessageId, taskMessageStorageRef);
            try (final var inputStream = dataStore.retrieve(taskMessageStorageRef)) {
                return inputStream.readAllBytes();
            } catch (final IOException | DataStoreException e) {
                final RabbitTaskInformation taskInformation = new RabbitTaskInformation(String.valueOf(inboundMessageId), true);
                LOG.error("Cannot register new message, rejecting storageRef:{} inbound messageid: {}",
                          taskMessageStorageRef, inboundMessageId, e);
                taskInformation.incrementResponseCount(true);
                final var publishHeaders = new HashMap<String, Object>();
                publishHeaders.put(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_REJECTED,
                                   REJECTED_REASON_PAYLOAD_OFFLOADING_TASKMESSAGE_DATASTORE_ERROR);
                publishHeaders.put(RabbitHeaders.RABBIT_HEADER_CAF_PAYLOAD_OFFLOADING_STORAGE_REF,
                                   taskMessageStorageRef);
                publisherEventQueue.add(
                    new WorkerPublishQueueEvent(delivery.getMessageData(), retryRoutingKey, taskInformation, publishHeaders));
                return null;
            }
        } else {
            return delivery.getMessageData();
        }
    }

    private TaskMessage deserializeTaskMessage(
        final byte[] taskMessageData,
        final long inboundMessageId,
        final Optional<String> taskMessageStorageRefOpt
    ) {
        try {
            return codec.deserialise(taskMessageData, TaskMessage.class, DecodeMethod.LENIENT);
        } catch (final CodecException e) {
            final RabbitTaskInformation errorTaskInformation = new RabbitTaskInformation(String.valueOf(inboundMessageId), true);
            LOG.error("Cannot register new message, rejecting {}", inboundMessageId, e);
            errorTaskInformation.incrementResponseCount(true);
            final var publishHeaders = new HashMap<String, Object>();
            publishHeaders.put(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_REJECTED, REJECTED_REASON_TASKMESSAGE);
            publishHeaders.put(RabbitHeaders.RABBIT_HEADER_CAF_PAYLOAD_OFFLOADING_STORAGE_REF, taskMessageStorageRefOpt.orElse(null));
            publisherEventQueue.add(new WorkerPublishQueueEvent(taskMessageData, retryRoutingKey, errorTaskInformation, publishHeaders));
            return null;
        }
    }

    private PoisonMessageStatus getPoisonMessageStatus(
        final boolean isRedelivered,
        final long inboundMessageId,
        final byte[] taskMessageByteArray,
        final Map<String, Object> deliveryHeaders,
        final int retries,
        final TrackingInfo trackingInfo
    ) {
        // If the message is being redelivered it is potentially a poison message.
        if (isRedelivered) {
            // If the headers do not contain the delivery count, then it is a classic queue.
            if (!deliveryHeaders.containsKey(RabbitHeaders.RABBIT_HEADER_CAF_DELIVERY_COUNT)) {
                // If the retries have not been exceeded, then republish the message
                // with a header recording the retry count
                if (retries < retryLimit) {
                    republishClassicRedelivery(inboundMessageId, taskMessageByteArray, retries, trackingInfo);
                    return PoisonMessageStatus.CLASSIC_AND_REPUBLISHED;
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
            publishHeaders.put(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_REJECTED, REJECTED_REASON_TASKMESSAGE);
            publisherEventQueue.add(new WorkerPublishQueueEvent(taskMessageByteArray, retryRoutingKey, taskInformation, publishHeaders));
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

        final String datastorePayloadReference = offloadedPayloads.get(tag);
        if (datastorePayloadReference != null) {
            try {
                dataStore.delete(datastorePayloadReference);
            } catch (final DataStoreException e) {
                LOG.warn("Couldn't delete offloaded payload '{}' for delivery tag '{}' from datastore message.",
                         datastorePayloadReference, tag, e);
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
        final long inboundMessageId,
        final byte[] taskMessageByteArray,
        final int retries,
        final TrackingInfo tracking
    )
    {
        final String trackingJobTaskId = tracking != null ? tracking.getJobTaskId() : "untracked";
        final RabbitTaskInformation taskInformation = new RabbitTaskInformation(
            String.valueOf(inboundMessageId), false, Optional.of(trackingJobTaskId));
        LOG.debug("Received redelivered message with id {}, retry count {}, retry limit {}, republishing to retry queue",
                  inboundMessageId, retryLimit, retries + 1);
        final Map<String, Object> headers = new HashMap<>();
        headers.put(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY, String.valueOf(retries + 1));
        taskInformation.incrementResponseCount(true);
        publisherEventQueue.add(new WorkerPublishQueueEvent(taskMessageByteArray, retryRoutingKey, taskInformation, headers));
    }
}
