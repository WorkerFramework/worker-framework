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

import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskCallback;
import com.hpe.caf.api.worker.TaskRejectedException;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.util.rabbitmq.*;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class RabbitWorkerQueueConsumerTest
{
    private String testQueue = "testQueue";
    private long id = 101L;
    private byte[] data = "test123".getBytes(StandardCharsets.UTF_8);
    private Envelope newEnv = new Envelope(id, false, "", testQueue);
    private Envelope redeliveredEnv = new Envelope(id, true, "", testQueue);
    private String retryKey = "retry";
    private RabbitMetricsReporter metrics = new RabbitMetricsReporter();
    private TaskCallback mockCallback = Mockito.mock(TaskCallback.class);

    /**
     * Send in a new message and verify the task registration callback is performed.
     */
    @Test
    public void testHandleDelivery()
        throws IOException, InterruptedException, WorkerException
    {
        BlockingQueue<Event<QueueConsumer>> consumerEvents = new LinkedBlockingQueue<>();
        BlockingQueue<Event<WorkerPublisher>> publisherEvents = new LinkedBlockingQueue<>();
        Channel channel = Mockito.mock(Channel.class);
        CountDownLatch latch = new CountDownLatch(1);
        TaskCallback callback = Mockito.mock(TaskCallback.class);
        Answer<Void> a = invocationOnMock -> {
            latch.countDown();
            return null;
        };
        Mockito.doAnswer(a).when(callback).registerNewTask(Mockito.any(), Mockito.any(), Mockito.anyMap());
        WorkerQueueConsumerImpl impl = new WorkerQueueConsumerImpl(callback, metrics, consumerEvents, channel, publisherEvents, retryKey, 1);
        DefaultRabbitConsumer consumer = new DefaultRabbitConsumer(consumerEvents, impl);
        Thread t = new Thread(consumer);
        t.start();
        AMQP.BasicProperties prop = Mockito.mock(AMQP.BasicProperties.class);
        Mockito.when(prop.getHeaders()).thenReturn(Collections.emptyMap());
        consumer.handleDelivery("consumer", newEnv, prop, data);
        Assert.assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
        consumer.shutdown();
    }

    /**
     * Send in a new message and verify that if the task registration throws an InvalidTaskException that a new publish request to the
     * reject queue is sent.
     */
    @Test
    public void testHandleDeliveryInvalid()
        throws IOException, InterruptedException, WorkerException
    {
        BlockingQueue<Event<QueueConsumer>> consumerEvents = new LinkedBlockingQueue<>();
        BlockingQueue<Event<WorkerPublisher>> publisherEvents = new LinkedBlockingQueue<>();
        Channel channel = Mockito.mock(Channel.class);
        TaskCallback callback = Mockito.mock(TaskCallback.class);
        Answer<Void> a = invocationOnMock -> {
            throw new InvalidTaskException("blah");
        };
        Mockito.doAnswer(a).when(callback).registerNewTask(Mockito.any(), Mockito.any(), Mockito.anyMap());
        WorkerQueueConsumerImpl impl = new WorkerQueueConsumerImpl(callback, metrics, consumerEvents, channel, publisherEvents, retryKey, 1);
        DefaultRabbitConsumer consumer = new DefaultRabbitConsumer(consumerEvents, impl);
        Thread t = new Thread(consumer);
        t.start();
        AMQP.BasicProperties prop = Mockito.mock(AMQP.BasicProperties.class);
        Mockito.when(prop.getHeaders()).thenReturn(Collections.emptyMap());
        consumer.handleDelivery("consumer", newEnv, prop, data);
        Event<WorkerPublisher> pubEvent = publisherEvents.poll(1, TimeUnit.SECONDS);
        Assert.assertNotNull(pubEvent);
        WorkerPublisher publisher = Mockito.mock(WorkerPublisher.class);
        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        pubEvent.handleEvent(publisher);
        Mockito.verify(publisher, Mockito.times(1)).handlePublish(Mockito.eq(data), Mockito.eq(retryKey), Mockito.eq(id), captor.capture(), Mockito.eq(0));
        Assert.assertTrue(captor.getValue().containsKey(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_REJECTED));
        Assert.assertEquals(WorkerQueueConsumerImpl.REJECTED_REASON_TASKMESSAGE,
                            captor.getValue().get(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_REJECTED));
        consumer.shutdown();
    }

    /**
     * Send in a new message and verify that if the task registration throws a TaskRejectedException that a new publish request back to
     * the input queue is sent.
     */
    @Test
    public void testHandleDeliveryRejected()
        throws IOException, InterruptedException, WorkerException
    {
        BlockingQueue<Event<QueueConsumer>> consumerEvents = new LinkedBlockingQueue<>();
        BlockingQueue<Event<WorkerPublisher>> publisherEvents = new LinkedBlockingQueue<>();
        Channel channel = Mockito.mock(Channel.class);
        TaskCallback callback = Mockito.mock(TaskCallback.class);
        Answer<Void> a = invocationOnMock -> {
            throw new TaskRejectedException("blah");
        };
        Mockito.doAnswer(a).when(callback).registerNewTask(Mockito.any(), Mockito.any(), Mockito.anyMap());
        WorkerQueueConsumerImpl impl = new WorkerQueueConsumerImpl(callback, metrics, consumerEvents, channel, publisherEvents, retryKey, 1);
        DefaultRabbitConsumer consumer = new DefaultRabbitConsumer(consumerEvents, impl);
        Thread t = new Thread(consumer);
        t.start();
        AMQP.BasicProperties prop = Mockito.mock(AMQP.BasicProperties.class);
        Mockito.when(prop.getHeaders()).thenReturn(Collections.emptyMap());
        consumer.handleDelivery("consumer", newEnv, prop, data);
        Event<WorkerPublisher> pubEvent = publisherEvents.poll(1, TimeUnit.SECONDS);
        Assert.assertNotNull(pubEvent);
        WorkerPublisher publisher = Mockito.mock(WorkerPublisher.class);
        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        pubEvent.handleEvent(publisher);
        Mockito.verify(publisher, Mockito.times(1)).handlePublish(Mockito.eq(data), Mockito.eq(testQueue), Mockito.eq(id), captor.capture(), Mockito.eq(0));
        Assert.assertFalse(captor.getValue().containsKey(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_REJECTED));
        consumer.shutdown();
    }

    /**
     * Send in a message marked as redelivered and verify that a new publish request is sent to the retry queue with the appropriate
     * headers stamped.
     */
    @Test
    public void testHandleRedelivery()
        throws IOException, InterruptedException, WorkerException
    {
        BlockingQueue<Event<QueueConsumer>> consumerEvents = new LinkedBlockingQueue<>();
        BlockingQueue<Event<WorkerPublisher>> publisherEvents = new LinkedBlockingQueue<>();
        Channel channel = Mockito.mock(Channel.class);
        TaskCallback callback = Mockito.mock(TaskCallback.class);
        WorkerQueueConsumerImpl impl = new WorkerQueueConsumerImpl(callback, metrics, consumerEvents, channel, publisherEvents, retryKey, 1);
        DefaultRabbitConsumer consumer = new DefaultRabbitConsumer(consumerEvents, impl);
        Thread t = new Thread(consumer);
        t.start();
        AMQP.BasicProperties prop = Mockito.mock(AMQP.BasicProperties.class);
        Mockito.when(prop.getHeaders()).thenReturn(Collections.emptyMap());
        consumer.handleDelivery("consumer", redeliveredEnv, prop, data);
        Event<WorkerPublisher> pubEvent = publisherEvents.poll(1, TimeUnit.SECONDS);
        Assert.assertNotNull(pubEvent);
        WorkerPublisher publisher = Mockito.mock(WorkerPublisher.class);
        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        pubEvent.handleEvent(publisher);
        Mockito.verify(publisher, Mockito.times(1)).handlePublish(Mockito.eq(data), Mockito.eq(retryKey), Mockito.eq(id), captor.capture(), Mockito.eq(0));
        Assert.assertTrue(captor.getValue().containsKey(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY));
        Assert.assertEquals("1", captor.getValue().get(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY));
        consumer.shutdown();
    }

    /**
     * Send in a redelivered message with a retry header that exceeds the limit and verify a new publish request is sent to the rejected
     * queue with the appropriate headers stamped.
     */
    @Test
    public void testHandleRedeliveryRetryExceeded()
        throws IOException, InterruptedException, WorkerException
    {
        BlockingQueue<Event<QueueConsumer>> consumerEvents = new LinkedBlockingQueue<>();
        BlockingQueue<Event<WorkerPublisher>> publisherEvents = new LinkedBlockingQueue<>();
        Channel channel = Mockito.mock(Channel.class);
        TaskCallback callback = Mockito.mock(TaskCallback.class);
        WorkerQueueConsumerImpl impl = new WorkerQueueConsumerImpl(callback, metrics, consumerEvents, channel, publisherEvents, retryKey, 1);
        DefaultRabbitConsumer consumer = new DefaultRabbitConsumer(consumerEvents, impl);
        Thread t = new Thread(consumer);
        t.start();
        AMQP.BasicProperties prop = Mockito.mock(AMQP.BasicProperties.class);
        Map<String, Object> headers = new HashMap<>();
        headers.put(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY, "1");
        Mockito.when(prop.getHeaders()).thenReturn(headers);
        consumer.handleDelivery("consumer", redeliveredEnv, prop, data);
        Event<WorkerPublisher> pubEvent = publisherEvents.poll(1, TimeUnit.SECONDS);
        Assert.assertNotNull(pubEvent);
        WorkerPublisher publisher = Mockito.mock(WorkerPublisher.class);
        ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);
        pubEvent.handleEvent(publisher);
        Mockito.verify(publisher, Mockito.times(1)).handlePublish(Mockito.eq(data), Mockito.eq(retryKey), Mockito.eq(id), captor.capture(), Mockito.eq(0));
        Assert.assertTrue(captor.getValue().containsKey(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY));
        Assert.assertEquals("1", captor.getValue().get(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY));
        Assert.assertTrue(captor.getValue().containsKey(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_REJECTED));
        Assert.assertEquals(WorkerQueueConsumerImpl.REJECTED_REASON_RETRIES_EXCEEDED,
                            captor.getValue().get(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_REJECTED));
        consumer.shutdown();
    }

    /**
     * Verify an ack request sends the appropriate signal to RabbitMQ.
     */
    @Test
    public void testHandleDeliveryAck()
        throws IOException, InterruptedException, WorkerException
    {
        BlockingQueue<Event<QueueConsumer>> consumerEvents = new LinkedBlockingQueue<>();
        BlockingQueue<Event<WorkerPublisher>> publisherEvents = new LinkedBlockingQueue<>();
        CountDownLatch channelLatch = new CountDownLatch(1);
        Channel channel = Mockito.mock(Channel.class);
        Answer<Void> a = invocationOnMock -> {
            channelLatch.countDown();
            return null;
        };
        Mockito.doAnswer(a).when(channel).basicAck(Mockito.eq(id), Mockito.anyBoolean());
        WorkerQueueConsumerImpl impl = new WorkerQueueConsumerImpl(mockCallback, metrics, consumerEvents, channel, publisherEvents, retryKey, 1);
        DefaultRabbitConsumer consumer = new DefaultRabbitConsumer(consumerEvents, impl);
        Thread t = new Thread(consumer);
        t.start();
        consumerEvents.add(new ConsumerAckEvent(id));
        Assert.assertTrue(channelLatch.await(30000, TimeUnit.MILLISECONDS));
        consumer.shutdown();
    }

    /**
     * Verify a reject request sends the appropriate signal to RabbitMQ.
     */
    @Test
    public void testHandleDeliveryReject()
        throws IOException, InterruptedException, WorkerException
    {
        BlockingQueue<Event<QueueConsumer>> consumerEvents = new LinkedBlockingQueue<>();
        BlockingQueue<Event<WorkerPublisher>> publisherEvents = new LinkedBlockingQueue<>();
        CountDownLatch channelLatch = new CountDownLatch(1);
        Channel channel = Mockito.mock(Channel.class);
        Answer<Void> a = invocationOnMock -> {
            channelLatch.countDown();
            return null;
        };
        Mockito.doAnswer(a).when(channel).basicReject(Mockito.eq(id), Mockito.eq(true));
        WorkerQueueConsumerImpl impl = new WorkerQueueConsumerImpl(mockCallback, metrics, consumerEvents, channel, publisherEvents, retryKey, 1);
        DefaultRabbitConsumer consumer = new DefaultRabbitConsumer(consumerEvents, impl);
        Thread t = new Thread(consumer);
        t.start();
        consumerEvents.add(new ConsumerRejectEvent(id));
        Assert.assertTrue(channelLatch.await(1000, TimeUnit.MILLISECONDS));
        consumer.shutdown();
    }

    /**
     * Verify a drop request sends the appropriate signal to RabbitMQ.
     */
    @Test
    public void testHandleDeliveryDrop()
        throws IOException, InterruptedException, WorkerException
    {
        BlockingQueue<Event<QueueConsumer>> consumerEvents = new LinkedBlockingQueue<>();
        BlockingQueue<Event<WorkerPublisher>> publisherEvents = new LinkedBlockingQueue<>();
        CountDownLatch channelLatch = new CountDownLatch(1);
        Channel channel = Mockito.mock(Channel.class);
        Answer<Void> a = invocationOnMock -> {
            channelLatch.countDown();
            return null;
        };
        Mockito.doAnswer(a).when(channel).basicReject(id, false);
        WorkerQueueConsumerImpl impl = new WorkerQueueConsumerImpl(mockCallback, metrics, consumerEvents, channel, publisherEvents, retryKey, 1);
        DefaultRabbitConsumer consumer = new DefaultRabbitConsumer(consumerEvents, impl);
        Thread t = new Thread(consumer);
        t.start();
        consumerEvents.add(new ConsumerDropEvent(id));
        Assert.assertTrue(channelLatch.await(1000, TimeUnit.MILLISECONDS));
        consumer.shutdown();
    }

}
