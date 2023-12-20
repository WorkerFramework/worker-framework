/*
 * Copyright 2015-2024 Open Text.
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
package com.hpe.caf.worker.queue.rabbit;

import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskCallback;
import com.hpe.caf.api.worker.TaskRejectedException;
import com.hpe.caf.util.rabbitmq.QueueConsumer;
import com.hpe.caf.util.rabbitmq.ConsumerAckEvent;
import com.hpe.caf.util.rabbitmq.Event;
import com.hpe.caf.util.rabbitmq.Delivery;
import com.hpe.caf.util.rabbitmq.RabbitHeaders;
import com.hpe.caf.util.rabbitmq.ConsumerRejectEvent;
import com.hpe.caf.util.rabbitmq.ConsumerDropEvent;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

/**
 * QueueConsumer implementation for a WorkerQueue. This QueueConsumer hands off messages to worker-core upon delivery assuming the message
 * is not marked 'redelivered'. Redelivered messages are republished to the retry queue with an incremented retry count. Redelivered
 * messages that have exceeded the retry count are republished to the rejected queue.
 */
public class WorkerQueueConsumerImpl implements QueueConsumer
{
    public static final String REJECTED_REASON_TASKMESSAGE = "TASKMESSAGE_INVALID";
    public static final String REJECTED_REASON_RETRIES_EXCEEDED = "RETRIES_EXCEEDED";
    private final TaskCallback callback;
    private final RabbitMetricsReporter metrics;
    private final BlockingQueue<Event<QueueConsumer>> consumerEventQueue;
    private final BlockingQueue<Event<WorkerPublisher>> publisherEventQueue;
    private final Channel channel;
    private final String retryRoutingKey;
    private final int retryLimit;
    private static final Logger LOG = LoggerFactory.getLogger(WorkerQueueConsumerImpl.class);

    public WorkerQueueConsumerImpl(TaskCallback callback, RabbitMetricsReporter metrics, BlockingQueue<Event<QueueConsumer>> queue, Channel ch,
                                   BlockingQueue<Event<WorkerPublisher>> pubQueue, String retryKey, int retryLimit)
    {
        this.callback = Objects.requireNonNull(callback);
        this.metrics = Objects.requireNonNull(metrics);
        this.consumerEventQueue = Objects.requireNonNull(queue);
        this.channel = Objects.requireNonNull(ch);
        this.publisherEventQueue = Objects.requireNonNull(pubQueue);
        this.retryRoutingKey = Objects.requireNonNull(retryKey);
        this.retryLimit = retryLimit;
    }

    /**
     * {@inheritDoc}
     *
     * If an incoming message is marked as redelivered, hand it off to another method to deal with retry/rejection. Otherwise, hand it off
     * to worker-core, and potentially repbulish or reject it depending upon exceptions thrown.
     */
    @Override
    public void processDelivery(Delivery delivery)
    {
        final RabbitTaskInformation taskInformation = new RabbitTaskInformation(String.valueOf(delivery.getEnvelope().getDeliveryTag()));
        metrics.incrementReceived();
        if (delivery.getEnvelope().isRedeliver()) {
            final int retries;
            if(delivery.getHeaders().containsKey(RabbitHeaders.RABBIT_HEADER_CAF_DELIVERY_COUNT)) {
                //RABBIT_HEADER_CAF_DELIVERY_COUNT is available for messages from quorum queues
                retries = Integer.parseInt(String.valueOf(delivery.getHeaders().getOrDefault(
                        RabbitHeaders.RABBIT_HEADER_CAF_DELIVERY_COUNT, "0")));
            }
            else {
                //RABBIT_HEADER_CAF_WORKER_RETRY is available for classic queues
                retries = Integer.parseInt(String.valueOf(delivery.getHeaders()
                        .getOrDefault(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY, "0")));
                if(retries < retryLimit) {
                    //Republish the delivery with a header recording the incremented number of retries.
                    //Classic queues do not record delivery count, so we republish the message with an incremented
                    //retry count. This allows us to track the number of attempts to process the message.
                    republishClassicRedelivery(delivery, retries);
                    return;
                }
            }
            
            if(retries >= retryLimit) {
                handleRetriesExceeded(delivery, retries);
                return;
            }
        }

        try {
            LOG.debug("Registering new message {}", taskInformation.getInboundMessageId());
            callback.registerNewTask(taskInformation, delivery.getMessageData(), delivery.getHeaders());
        } catch (InvalidTaskException e) {
            LOG.error("Cannot register new message, rejecting {}", taskInformation.getInboundMessageId(), e);
            taskInformation.incrementResponseCount(true);
            publisherEventQueue.add(new WorkerPublishQueueEvent(delivery.getMessageData(), retryRoutingKey, taskInformation,
                    Collections.singletonMap(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_REJECTED, REJECTED_REASON_TASKMESSAGE)));
        } catch (TaskRejectedException e) {
            LOG.warn("Message {} rejected as a task at this time, returning to queue", taskInformation.getInboundMessageId(), e);
            taskInformation.incrementResponseCount(true);
            publisherEventQueue.add(new WorkerPublishQueueEvent(delivery.getMessageData(), delivery.getEnvelope().getRoutingKey(),
                    taskInformation));
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
     * @param delivery the redelivered message
     */
    private void republishClassicRedelivery(final Delivery delivery, final int retries) {

        final RabbitTaskInformation taskInformation = 
                new RabbitTaskInformation(String.valueOf(delivery.getEnvelope().getDeliveryTag()));
        LOG.debug("Received redelivered message with id {}, retry count {}, retry limit {}, republishing to retry queue",
                delivery.getEnvelope().getDeliveryTag(), retryLimit, retries + 1);
        final Map<String, Object> headers = new HashMap<>();
        headers.put(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY, String.valueOf(retries + 1));
        headers.put(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY_LIMIT, retryLimit);
        taskInformation.incrementResponseCount(true);
        publisherEventQueue.add(new WorkerPublishQueueEvent(delivery.getMessageData(), retryRoutingKey, 
                taskInformation, headers));
    }
    
    /**
     * Republish the delivery to the rejected queue with a rejected reason stamped in the headers
     *
     * @param delivery the redelivered message
     */
    private void handleRetriesExceeded(final Delivery delivery, final int retries) {
        final RabbitTaskInformation taskInformation = 
                new RabbitTaskInformation(String.valueOf(delivery.getEnvelope().getDeliveryTag()));
        LOG.debug("Retry exceeded for message with id {}, republishing to rejected queue", 
                delivery.getEnvelope().getDeliveryTag());
        final Map<String, Object> headers = new HashMap<>();
        headers.put(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY, String.valueOf(retries));
        headers.put(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_REJECTED, REJECTED_REASON_RETRIES_EXCEEDED);
        headers.put(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY_LIMIT, retryLimit);
        taskInformation.incrementResponseCount(true);
        publisherEventQueue.add(new WorkerPublishQueueEvent(delivery.getMessageData(), retryRoutingKey, 
                taskInformation, headers));
    }
}
