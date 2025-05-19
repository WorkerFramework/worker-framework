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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;

import static com.github.workerframework.util.rabbitmq.RabbitHeaders.RABBIT_HEADER_CAF_MINIMIZATION_ID;

/**
 * QueueConsumer implementation for a WorkerQueue. This QueueConsumer hands off messages to worker-core upon delivery assuming the message
 * is not marked 'redelivered'. Redelivered messages are republished to the retry queue with an incremented retry count. Redelivered
 * messages that have exceeded the retry count are republished to the rejected queue.
 */
public class WorkerQueueConsumerImpl implements QueueConsumer {
    public static final String REJECTED_REASON_TASKMESSAGE = "TASKMESSAGE_INVALID";
    public static final String REJECTED_REASON_MINIMIZED_TASKMESSAGE = "MINIMIZED_TASKMESSAGE_ID_INVALID";
    private final TaskCallback callback;
    private final RabbitMetricsReporter metrics;
    private final BlockingQueue<Event<QueueConsumer>> consumerEventQueue;
    private final BlockingQueue<Event<WorkerPublisher>> publisherEventQueue;
    private final Channel channel;
    private final String retryRoutingKey;
    private final int retryLimit;
    private final ManagedDataStore dataStore;
    private final Codec codec;
    private static final Logger LOG = LoggerFactory.getLogger(WorkerQueueConsumerImpl.class);

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
    }

    /**
     * {@inheritDoc}
     * <p>
     * If an incoming message is marked as redelivered, hand it off to another method to deal with retry/rejection. Otherwise, hand it off
     * to worker-core, and potentially repbulish or reject it depending upon exceptions thrown.
     */
    @Override
    public void processDelivery(Delivery delivery) {

        final var inboundMessageId = delivery.getEnvelope().getDeliveryTag();
        final var routingKey = delivery.getEnvelope().getRoutingKey();
        final var deliveryHeaders = delivery.getHeaders();

        final int retries = deliveryHeaders.containsKey(RabbitHeaders.RABBIT_HEADER_CAF_DELIVERY_COUNT) ?
            Integer.parseInt(String.valueOf(deliveryHeaders.getOrDefault(RabbitHeaders.RABBIT_HEADER_CAF_DELIVERY_COUNT, "0"))) :
            Integer.parseInt(String.valueOf(deliveryHeaders.getOrDefault(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY, "0")));

        final Optional<String> taskMessageStorageRefOpt = Optional.ofNullable(
            deliveryHeaders.get(RABBIT_HEADER_CAF_MINIMIZATION_ID)
        ).map(Object::toString);

        metrics.incrementReceived();
        final boolean isPoison;
        if (delivery.getEnvelope().isRedeliver()) {
            if (!deliveryHeaders.containsKey(RabbitHeaders.RABBIT_HEADER_CAF_DELIVERY_COUNT)) {
                //RABBIT_HEADER_CAF_DELIVERY_COUNT is not available, message was delivered from CLASSIC queue
                if (retries < retryLimit) {
                    //Republish the delivery with a header recording the incremented number of retries.
                    //Classic queues do not record delivery count, so we republish the message with an incremented
                    //retry count. This allows us to track the number of attempts to process the message.
                    republishClassicRedelivery(delivery, retries, taskMessageStorageRefOpt);
                    return;
                }
                isPoison = true;
            } else {
                isPoison = retries > retryLimit;
            }
        } else {
            isPoison = false;
        }

        final byte[] taskMessageData;
        if (taskMessageStorageRefOpt.isPresent()) {
            try {
                taskMessageData = retrieveFromDatastore(taskMessageStorageRefOpt.get());
            } catch (final IOException | DataStoreException e) {
                // The message was minimized, but we could not retrieve it from the data store.
                // we will not mark it for deletion as it does not exist,
                // if it does exist we add it in the header so can inspect it later.
                final RabbitTaskInformation taskInformation = new RabbitTaskInformation(
                    String.valueOf(inboundMessageId),
                    isPoison
                );
                LOG.error("Cannot register new message, rejecting storageRef:{} inbound messageid: {}",
                    taskMessageStorageRefOpt.get(), inboundMessageId, e);
                taskInformation.incrementResponseCount(true);
                final var publishHeaders = new HashMap<String, Object>();
                publishHeaders.put(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_REJECTED, REJECTED_REASON_TASKMESSAGE);
                publishHeaders.put(RabbitHeaders.RABBIT_HEADER_CAF_MINIMIZATION_REJECTED, REJECTED_REASON_MINIMIZED_TASKMESSAGE);
                publishHeaders.put(RabbitHeaders.RABBIT_HEADER_CAF_MINIMIZATION_REJECTED_ID, taskMessageStorageRefOpt.get());
                publisherEventQueue.add(new WorkerPublishQueueEvent(delivery.getMessageData(), retryRoutingKey, taskInformation, publishHeaders));
                return;
            }
        } else {
            taskMessageData = delivery.getMessageData();
        }
        processDelivery(
            inboundMessageId,
            routingKey,
            deliveryHeaders,
            taskMessageData,
            isPoison,
            taskMessageStorageRefOpt
        );
    }

    private void processDelivery(
        final long inboundMessageId,
        final String routingKey,
        final Map<String, Object> deliveryHeaders,
        final byte[] taskMessageByteArray,
        final boolean isPoison,
        final Optional<String> taskMessageStorageRefOpt
    )
    {
        final TaskMessage taskMessage;
        final RabbitTaskInformation taskInformation;
        try {
            taskMessage = codec.deserialise(taskMessageByteArray, TaskMessage.class, DecodeMethod.LENIENT);
            final var trackingInfo = taskMessage.getTracking();
            final var trackingJobTaskId = trackingInfo != null ? trackingInfo.getJobTaskId() : "untracked";
            taskInformation = new RabbitTaskInformation(
                String.valueOf(inboundMessageId),
                isPoison,
                taskMessageStorageRefOpt,
                Optional.of(trackingJobTaskId)
            );
        } catch (final CodecException e) {
            final RabbitTaskInformation errorTaskInformation = new RabbitTaskInformation(
                String.valueOf(inboundMessageId),
                isPoison,
                taskMessageStorageRefOpt,
                Optional.empty()
            );
            LOG.error("Cannot register new message, rejecting {}", inboundMessageId, e);
            errorTaskInformation.incrementResponseCount(true);
            final var publishHeaders = new HashMap<String, Object>();
            publishHeaders.put(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_REJECTED, REJECTED_REASON_TASKMESSAGE);
            publisherEventQueue.add(new WorkerPublishQueueEvent(taskMessageByteArray, retryRoutingKey, errorTaskInformation, publishHeaders));
            return;
        }

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

    private byte[] retrieveFromDatastore(final String taskMessageStorageRef) throws DataStoreException, IOException {
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (final var inputStream = dataStore.retrieve(taskMessageStorageRef)) {
            final byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
        }
        return outputStream.toByteArray();
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

    /**
     * Republish the delivery to the retry queue with the retry count stamped in the headers.
     * 
     * @param delivery The redelivered message
     * @param retries
     * @param taskMessageStorageRefOpt
     */
    private void republishClassicRedelivery(final Delivery delivery, final int retries, final Optional<String> taskMessageStorageRefOpt) {

        final RabbitTaskInformation taskInformation = 
                new RabbitTaskInformation(
                    String.valueOf(delivery.getEnvelope().getDeliveryTag()),
                    false,
                    taskMessageStorageRefOpt,
                    Optional.empty()
                );
        LOG.debug("Received redelivered message with id {}, retry count {}, retry limit {}, republishing to retry queue",
                delivery.getEnvelope().getDeliveryTag(), retryLimit, retries + 1);
        final Map<String, Object> headers = new HashMap<>();
        headers.put(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY, String.valueOf(retries + 1));
        taskInformation.incrementResponseCount(true);
        publisherEventQueue.add(new WorkerPublishQueueEvent(delivery.getMessageData(), retryRoutingKey, 
                taskInformation, headers));
    }
}
