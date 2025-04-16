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
import com.github.workerframework.api.DataStoreException;
import com.github.workerframework.api.ManagedDataStore;
import com.github.workerframework.api.QueueException;
import com.github.workerframework.api.TaskMessage;
import com.github.workerframework.util.rabbitmq.ConsumerRejectEvent;
import com.github.workerframework.util.rabbitmq.Event;
import com.github.workerframework.util.rabbitmq.QueueConsumer;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;

/**
 * A RabbitMQ publisher that uses a ConfirmListener, sending data as plain text with headers. Messages that cannot be published at all
 * cause a rejection of the input message (task) that triggered this published response.
 */
public class WorkerPublisherImpl implements WorkerPublisher
{
    private final Channel channel;
    private final RabbitMetricsReporter metrics;
    private final BlockingQueue<Event<QueueConsumer>> consumerEvents;
    private final WorkerConfirmListener confirmListener;
    private final ManagedDataStore dataStore;
    private final RabbitWorkerQueueConfiguration config;
    private final Codec codec;
    public static final String DEHYDRATED_MESSAGE_TASK_NAME = "DehydratedMessageTask";
    private static final Logger LOG = LoggerFactory.getLogger(WorkerPublisherImpl.class);

    /**
     * Create a WorkerPublisher implementation. The channel will have confirmations turned on and the supplied WorkerConfirmListener will
     * be added as a confirm listener upon the channel.
     *
     * @param ch the channel to use, will have confirmations enabled
     * @param metrics the metrics to report to
     * @param events the event queue of the consumer to ack/reject on
     * @param listener the listener callback that accepts ack/nack publisher confirms from the broker
     * @throws IOException if the channel cannot have confirmations enabled
     */
    public WorkerPublisherImpl(
        Channel ch,
        RabbitMetricsReporter metrics,
        BlockingQueue<Event<QueueConsumer>> events,
        WorkerConfirmListener listener,
        ManagedDataStore dataStore,
        RabbitWorkerQueueConfiguration config,
        Codec codec
    ) throws IOException
    {
        this.channel = Objects.requireNonNull(ch);
        this.metrics = Objects.requireNonNull(metrics);
        this.consumerEvents = Objects.requireNonNull(events);
        this.confirmListener = Objects.requireNonNull(listener);
        this.dataStore = Objects.requireNonNull(dataStore);
        this.config = Objects.requireNonNull(config);
        this.codec = Objects.requireNonNull(codec);
        channel.confirmSelect();
        channel.addConfirmListener(confirmListener);
    }

    @Override
    public void handlePublish(byte[] data, String routingKey, RabbitTaskInformation taskInformation, Map<String, Object> headers)
    {
        try {
            LOG.debug("Publishing message to {} with ack id {}", routingKey, taskInformation.getInboundMessageId());
            AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties().builder();
            builder.headers(headers);
            builder.contentType("text/plain");
            builder.deliveryMode(2);
            confirmListener.registerResponseSequence(channel.getNextPublishSeqNo(), taskInformation);
            final var outboundTaskMessage = getOutboundTaskMessage(data, routingKey);
            channel.basicPublish("", routingKey, builder.build(), outboundTaskMessage);
            metrics.incrementPublished();
            deleteStoredMessage(taskInformation);
        } catch (final IOException | QueueException e) {
            LOG.error("Failed to publish result of message {} to queue {}, rejecting", taskInformation.getInboundMessageId(), routingKey, e);
            metrics.incremementErrors();
            consumerEvents.add(new ConsumerRejectEvent(Long.valueOf(taskInformation.getInboundMessageId())));
        }
    }

    private void deleteStoredMessage(final RabbitTaskInformation taskInformation)
    {
        final var rehydratedMessageIdOpt = taskInformation.getRehydratedMessageId();
        if (rehydratedMessageIdOpt.isEmpty()) {
            return;
        }
        try {
            dataStore.delete(rehydratedMessageIdOpt.get());
        } catch (final DataStoreException e) {
            LOG.error("Failed to delete a stored message id:{} from the datastore", rehydratedMessageIdOpt.get(), e);
        }
    }

    private boolean shouldStoreTaskMessage(final int taskMessageSize) {
        return config.getMessageDehydrationConfig().isEnabled() &&
            taskMessageSize > config.getMessageDehydrationConfig().getThreshold();
    }

    private byte[] getOutboundTaskMessage(final byte[] taskMessage, final String routingKey) throws QueueException {
        try {
            if (shouldStoreTaskMessage(taskMessage.length)) {
                final TaskMessage outgoingTaskMessage = codec.deserialise(taskMessage, TaskMessage.class);
                final var taskMessagePartialRef = String.format("%s/%s", routingKey, outgoingTaskMessage.getTracking().getJobTaskId());
                final var dehydratedMessageId = dataStore.store(taskMessage, taskMessagePartialRef);

                outgoingTaskMessage.setTaskClassifier(DEHYDRATED_MESSAGE_TASK_NAME);
                outgoingTaskMessage.setTaskData(dehydratedMessageId.getBytes(StandardCharsets.UTF_8));
                return codec.serialise(outgoingTaskMessage);
            }
        } catch (final Exception e) {
            throw new QueueException("Error dehydrating task message", e);
        }
        return taskMessage;
    }
}
