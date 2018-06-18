/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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

import com.hpe.caf.util.rabbitmq.ConsumerRejectEvent;
import com.hpe.caf.util.rabbitmq.Event;
import com.hpe.caf.util.rabbitmq.QueueConsumer;
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
 * A RabbitMQ publisher that uses a ConfirmListener, sending data as plain text with headers. Messages that cannot be published at all
 * cause a rejection of the input message (task) that triggered this published response.
 */
public class WorkerPublisherImpl implements WorkerPublisher
{
    private final Channel channel;
    private final RabbitMetricsReporter metrics;
    private final BlockingQueue<Event<QueueConsumer>> consumerEvents;
    private final WorkerConfirmListener confirmListener;
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
    public WorkerPublisherImpl(Channel ch, RabbitMetricsReporter metrics, BlockingQueue<Event<QueueConsumer>> events, WorkerConfirmListener listener)
        throws IOException
    {
        this.channel = Objects.requireNonNull(ch);
        this.metrics = Objects.requireNonNull(metrics);
        this.consumerEvents = Objects.requireNonNull(events);
        this.confirmListener = Objects.requireNonNull(listener);
        channel.confirmSelect();
        channel.addConfirmListener(confirmListener);
    }

    @Override
    public void handlePublish(byte[] data, String routingKey, long ackId, Map<String, Object> headers, int priority)
    {
        try {
            LOG.debug("Publishing message with ack id {}", ackId);
            AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties().builder();
            builder.headers(headers);
            builder.contentType("text/plain");
            builder.deliveryMode(2);
            builder.priority(priority);
            confirmListener.registerResponseSequence(channel.getNextPublishSeqNo(), ackId);
            channel.basicPublish("", routingKey, builder.build(), data);
            metrics.incrementPublished();
        } catch (IOException e) {
            LOG.error("Failed to publish result of message {} to queue {}, rejecting", ackId, routingKey, e);
            metrics.incremementErrors();
            consumerEvents.add(new ConsumerRejectEvent(ackId));
        }
    }
}
