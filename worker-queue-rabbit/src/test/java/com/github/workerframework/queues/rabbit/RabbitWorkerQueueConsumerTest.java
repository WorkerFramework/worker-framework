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
import com.github.cafapi.common.codecs.json.JsonCodec;
import com.github.workerframework.api.DataStoreException;
import com.github.workerframework.api.DeletableTree;
import com.github.workerframework.api.InvalidTaskException;
import com.github.workerframework.api.ManagedDataStore;
import com.github.workerframework.api.TaskCallback;
import com.github.workerframework.api.TaskInformation;
import com.github.workerframework.api.TaskMessage;
import com.github.workerframework.api.TaskRejectedException;
import com.github.workerframework.api.TaskStatus;
import com.github.workerframework.api.TrackingInfo;
import com.github.workerframework.api.WorkerException;
import com.github.workerframework.datastores.fs.FileSystemDataStore;
import com.github.workerframework.datastores.fs.FileSystemDataStoreConfiguration;
import com.github.workerframework.util.rabbitmq.ConsumerAckEvent;
import com.github.workerframework.util.rabbitmq.ConsumerDropEvent;
import com.github.workerframework.util.rabbitmq.ConsumerRejectEvent;
import com.github.workerframework.util.rabbitmq.DefaultRabbitConsumer;
import com.github.workerframework.util.rabbitmq.Event;
import com.github.workerframework.util.rabbitmq.QueueConsumer;
import com.github.workerframework.util.rabbitmq.RabbitHeaders;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;

public class RabbitWorkerQueueConsumerTest
{
    private String testQueue = "testQueue";
    private RabbitTaskInformation taskInformation;    
    private Envelope newEnv;
    private Envelope poisonEnv;
    private Envelope redeliveredEnv;
    private String retryKey = "retry";
    private String invalidKey = "invalid";
    private RabbitMetricsReporter metrics = new RabbitMetricsReporter();
    private TaskCallback mockCallback = mock(TaskCallback.class);
    private File tempDataStore;
    private File queuesDirectory;
    private ManagedDataStore dataStore;
    private static Codec codec;
    private static byte[] data;

    @BeforeClass
    public static void beforeClass() throws CodecException {
        codec = new JsonCodec();
        data = getNewTaskMessage();
    }

    @BeforeMethod
    public void beforeMethod() throws DataStoreException {
        taskInformation = new RabbitTaskInformation("101");
        newEnv = new Envelope(Long.valueOf(taskInformation.getInboundMessageId()), false, "", testQueue);
        poisonEnv = new Envelope(Long.valueOf(taskInformation.getInboundMessageId()), true, "", testQueue);
        redeliveredEnv = new Envelope(Long.valueOf(taskInformation.getInboundMessageId()), true, "", testQueue);
        tempDataStore = new File("datastore");
        queuesDirectory = new File("datastore/queues/");
        dataStore = new FileSystemDataStore(createConfig());
    }

    @AfterMethod
    public void tearDown()
    {
        deleteDir(tempDataStore);
    }

    private void deleteDir(File file)
    {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f);
            }
        }
        file.delete();
    }

    private FileSystemDataStoreConfiguration createConfig()
    {
        final FileSystemDataStoreConfiguration conf = new FileSystemDataStoreConfiguration();
        conf.setDataDir(tempDataStore.getAbsolutePath());
        conf.setDataDirHealthcheckTimeoutSeconds(10);
        return conf;
    }

    @Test
    public void testOffloadedEmptyDirectoriesDeleted() throws DataStoreException {
        final String trackingJobTaskId = "job/tracking/id/1";
        final String partialRef = "queues/" + trackingJobTaskId;
        final String message = UUID.randomUUID().toString();
        final String taskMessageStorageRef = dataStore.store(message.getBytes(), partialRef);
        final var deletableTree = (DeletableTree) dataStore;
        deletableTree.deleteTree(taskMessageStorageRef);
        Assert.assertTrue(Files.exists(tempDataStore.toPath()),
                "Should not have deleted datastore directory");
        Assert.assertFalse(Files.exists(queuesDirectory.toPath()),
                "Should have deleted queues directory and children");
    }

    @Test
    public void testOffloadedNonEmptyOffloadedDirectoriesNotDeleted() throws DataStoreException {
        final String trackingJobTaskId = "job/tracking/id/1";
        final String partialRef = "queues/" + trackingJobTaskId;
        final String undeletedPartialRef = "queues/";
        final String message = UUID.randomUUID().toString();
        final String taskMessageStorageRef = dataStore.store(message.getBytes(), partialRef);
        dataStore.store(message.getBytes(), undeletedPartialRef);
        final var deletableTree = (DeletableTree) dataStore;
        deletableTree.deleteTree(taskMessageStorageRef);
        Assert.assertTrue(Files.exists(tempDataStore.toPath()),
                "Should not have deleted datastore directory");
        // queues directory will not be deleted since it's not empty.
        Assert.assertTrue(Files.exists(queuesDirectory.toPath()),
                "Should have deleted queues directory children, but not queues directory");
    }

    /**
     * Send in a new message and verify the task registration callback is performed.
     */
    @Test
    public void testHandleDelivery()
        throws IOException, InterruptedException, WorkerException
    {
        BlockingQueue<Event<QueueConsumer>> consumerEvents = new LinkedBlockingQueue<>();
        BlockingQueue<Event<WorkerPublisher>> publisherEvents = new LinkedBlockingQueue<>();
        Channel channel = mock(Channel.class);
        CountDownLatch latch = new CountDownLatch(1);
        TaskCallback callback = mock(TaskCallback.class);
        Answer<Void> a = invocationOnMock -> {
            latch.countDown();
            return null;
        };
        Mockito.doAnswer(a).when(callback).registerNewTask(Mockito.any(), Mockito.any(), Mockito.anyMap());
        WorkerQueueConsumerImpl impl = new WorkerQueueConsumerImpl(
            callback, metrics, consumerEvents, channel, publisherEvents, retryKey, 1, invalidKey,
            dataStore, codec, () -> {}, "missingOffloadedPayloadQueue");
        DefaultRabbitConsumer consumer = new DefaultRabbitConsumer(consumerEvents, impl);
        Thread t = new Thread(consumer);
        t.start();
        AMQP.BasicProperties prop = mock(AMQP.BasicProperties.class);
        Mockito.when(prop.getHeaders()).thenReturn(Collections.emptyMap());
        consumer.handleDelivery("consumer", newEnv, prop, data);
        Assert.assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
        consumer.shutdown();
    }

    /**
     * Send in a message that has been retried once with retry limit set to 1, and verify the task information marks the message as
     * poisonous.
     */
    @Test
    public void testPoisonDelivery()
            throws InterruptedException, WorkerException
    {
        BlockingQueue<Event<QueueConsumer>> consumerEvents = new LinkedBlockingQueue<>();
        BlockingQueue<Event<WorkerPublisher>> publisherEvents = new LinkedBlockingQueue<>();
        Channel channel = mock(Channel.class);
        CountDownLatch latch = new CountDownLatch(1);
        TaskCallback callback = mock(TaskCallback.class);
        Answer<Void> a = invocationOnMock -> {
            latch.countDown();
            return null;
        };
        Mockito.doAnswer(a).when(callback).registerNewTask(Mockito.any(), Mockito.any(), Mockito.anyMap());
        WorkerQueueConsumerImpl impl = new WorkerQueueConsumerImpl(
            callback, metrics, consumerEvents, channel, publisherEvents, retryKey, 1, invalidKey,
            dataStore, codec, () -> {}, "missingOffloadedPayloadQueue");
        DefaultRabbitConsumer consumer = new DefaultRabbitConsumer(consumerEvents, impl);
        Thread t = new Thread(consumer);
        t.start();
        AMQP.BasicProperties prop = mock(AMQP.BasicProperties.class);
        Map<String, Object> headers = new HashMap<>();
        headers.put(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY, "1");
        Mockito.when(prop.getHeaders()).thenReturn(headers);
        consumer.handleDelivery("consumer", poisonEnv, prop, data);
        Assert.assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
        ArgumentCaptor<TaskInformation> taskInfoCaptor = ArgumentCaptor.forClass(TaskInformation.class);
        Mockito.verify(callback).registerNewTask(taskInfoCaptor.capture(), Mockito.any(), Mockito.any());
        Assert.assertTrue(taskInfoCaptor.getValue().isPoison());
        Assert.assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
        consumer.shutdown();
    }

    /**
     * Send in a new message and verify that if the task registration throws an InvalidTaskException that a new publish
     * request to the invalid queue is sent.
     */
    @Test
    public void testHandleDeliveryInvalid()
        throws IOException, InterruptedException, WorkerException
    {
        BlockingQueue<Event<QueueConsumer>> consumerEvents = new LinkedBlockingQueue<>();
        BlockingQueue<Event<WorkerPublisher>> publisherEvents = new LinkedBlockingQueue<>();
        Channel channel = mock(Channel.class);
        TaskCallback callback = mock(TaskCallback.class);
        Answer<Void> a = invocationOnMock -> {
            throw new InvalidTaskException("blah");
        };
        Mockito.doAnswer(a).when(callback).registerNewTask(Mockito.any(), Mockito.any(), Mockito.anyMap());
        WorkerQueueConsumerImpl impl = new WorkerQueueConsumerImpl(
            callback, metrics, consumerEvents, channel, publisherEvents, retryKey, 1, invalidKey,
            dataStore, codec, () -> {}, "missingOffloadedPayloadQueue");
        DefaultRabbitConsumer consumer = new DefaultRabbitConsumer(consumerEvents, impl);
        Thread t = new Thread(consumer);
        t.start();
        AMQP.BasicProperties prop = mock(AMQP.BasicProperties.class);
        Mockito.when(prop.getHeaders()).thenReturn(Collections.emptyMap());
        consumer.handleDelivery("consumer", newEnv, prop, data);
        Event<WorkerPublisher> pubEvent = publisherEvents.poll(1, TimeUnit.SECONDS);
        Assert.assertNotNull(pubEvent);
        WorkerPublisher publisher = mock(WorkerPublisher.class);
        ArgumentCaptor<Map<String, Object>> captor = buildStringObjectMapCaptor();
        pubEvent.handleEvent(publisher);
        Mockito.verify(publisher, Mockito.times(1)).handlePublish(Mockito.eq(data), Mockito.eq(invalidKey), Mockito.any(RabbitTaskInformation.class), captor.capture());
        Assert.assertTrue(captor.getValue().containsKey(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_INVALID));
        Assert.assertEquals(captor.getValue().get(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_INVALID).toString(),
                "com.github.workerframework.api.InvalidTaskException: blah");
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
        Channel channel = mock(Channel.class);
        TaskCallback callback = mock(TaskCallback.class);
        Answer<Void> a = invocationOnMock -> {
            throw new TaskRejectedException("blah");
        };
        Mockito.doAnswer(a).when(callback).registerNewTask(Mockito.any(), Mockito.any(), Mockito.anyMap());
        WorkerQueueConsumerImpl impl = new WorkerQueueConsumerImpl(
            callback, metrics, consumerEvents, channel, publisherEvents, retryKey, 1, invalidKey,
            dataStore, codec, () -> {}, "missingOffloadedPayloadQueue");
        DefaultRabbitConsumer consumer = new DefaultRabbitConsumer(consumerEvents, impl);
        Thread t = new Thread(consumer);
        t.start();
        AMQP.BasicProperties prop = mock(AMQP.BasicProperties.class);
        Mockito.when(prop.getHeaders()).thenReturn(Collections.emptyMap());
        consumer.handleDelivery("consumer", newEnv, prop, data);
        Event<WorkerPublisher> pubEvent = publisherEvents.poll(1, TimeUnit.SECONDS);
        Assert.assertNotNull(pubEvent);
        WorkerPublisher publisher = mock(WorkerPublisher.class);
        ArgumentCaptor<Map<String, Object>> captor = buildStringObjectMapCaptor();
        pubEvent.handleEvent(publisher);
        Mockito.verify(publisher, Mockito.times(1)).handlePublish(Mockito.eq(data), Mockito.eq(testQueue), Mockito.any(RabbitTaskInformation.class), captor.capture());
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
        Channel channel = mock(Channel.class);
        TaskCallback callback = mock(TaskCallback.class);
        WorkerQueueConsumerImpl impl = new WorkerQueueConsumerImpl(
            callback, metrics, consumerEvents, channel, publisherEvents, retryKey, 1, invalidKey,
            dataStore, codec, () -> {}, "missingOffloadedPayloadQueue");
        DefaultRabbitConsumer consumer = new DefaultRabbitConsumer(consumerEvents, impl);
        Thread t = new Thread(consumer);
        t.start();
        AMQP.BasicProperties prop = mock(AMQP.BasicProperties.class);
        Mockito.when(prop.getHeaders()).thenReturn(Collections.emptyMap());
        consumer.handleDelivery("consumer", redeliveredEnv, prop, data);
        Event<WorkerPublisher> pubEvent = publisherEvents.poll(1, TimeUnit.SECONDS);
        Assert.assertNotNull(pubEvent);
        WorkerPublisher publisher = mock(WorkerPublisher.class);
        ArgumentCaptor<Map<String, Object>> captor = buildStringObjectMapCaptor();
        pubEvent.handleEvent(publisher);
        Mockito.verify(publisher, Mockito.times(1)).handlePublish(Mockito.eq(data), Mockito.eq(retryKey), Mockito.any(RabbitTaskInformation.class), captor.capture());
        Assert.assertTrue(captor.getValue().containsKey(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY));
        Assert.assertEquals("1", captor.getValue().get(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY));
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
        Channel channel = mock(Channel.class);
        Answer<Void> a = invocationOnMock -> {
            channelLatch.countDown();
            return null;
        };
        Mockito.doAnswer(a).when(channel).basicAck(Mockito.eq(Long.valueOf(taskInformation.getInboundMessageId())), Mockito.anyBoolean());
        WorkerQueueConsumerImpl impl = new WorkerQueueConsumerImpl(
            mockCallback, metrics, consumerEvents, channel, publisherEvents, retryKey, 1, invalidKey,
            dataStore, codec, () -> {}, "missingOffloadedPayloadQueue");
        DefaultRabbitConsumer consumer = new DefaultRabbitConsumer(consumerEvents, impl);
        Thread t = new Thread(consumer);
        t.start();
        consumerEvents.add(new ConsumerAckEvent(Long.valueOf(taskInformation.getInboundMessageId())));
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
        Channel channel = mock(Channel.class);
        Answer<Void> a = invocationOnMock -> {
            channelLatch.countDown();
            return null;
        };
        Mockito.doAnswer(a).when(channel).basicReject(Mockito.eq(Long.valueOf(taskInformation.getInboundMessageId())), Mockito.eq(true));
        WorkerQueueConsumerImpl impl = new WorkerQueueConsumerImpl(
            mockCallback, metrics, consumerEvents, channel, publisherEvents, retryKey, 1, invalidKey,
            dataStore, codec, () -> {}, "missingOffloadedPayloadQueue");
        DefaultRabbitConsumer consumer = new DefaultRabbitConsumer(consumerEvents, impl);
        Thread t = new Thread(consumer);
        t.start();
        consumerEvents.add(new ConsumerRejectEvent(Long.valueOf(taskInformation.getInboundMessageId())));
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
        Channel channel = mock(Channel.class);
        Answer<Void> a = invocationOnMock -> {
            channelLatch.countDown();
            return null;
        };
        Mockito.doAnswer(a).when(channel).basicReject(Long.valueOf(taskInformation.getInboundMessageId()), false);
        WorkerQueueConsumerImpl impl = new WorkerQueueConsumerImpl(
            mockCallback, metrics, consumerEvents, channel, publisherEvents, retryKey, 1, invalidKey,
            dataStore, codec, () -> {}, "missingOffloadedPayloadQueue");
        DefaultRabbitConsumer consumer = new DefaultRabbitConsumer(consumerEvents, impl);
        Thread t = new Thread(consumer);
        t.start();
        consumerEvents.add(new ConsumerDropEvent(Long.valueOf(taskInformation.getInboundMessageId())));
        Assert.assertTrue(channelLatch.await(1000, TimeUnit.MILLISECONDS));
        consumer.shutdown();
    }

    @SuppressWarnings("unchecked")
    private static ArgumentCaptor<Map<String, Object>> buildStringObjectMapCaptor()
    {
        return ArgumentCaptor.forClass(Map.class);
    }
    
    private static byte[] getNewTaskMessage() throws CodecException {
        final var trackingInfo = new TrackingInfo("task1", new Date(), 1, "http://hello.com", "pipe", "to");
        return codec.serialise(new TaskMessage(
            "task1",
            "ACTUAL_CLASSIFIER",
            1,
            "test123".getBytes(StandardCharsets.UTF_8),
            TaskStatus.NEW_TASK,
            new HashMap<>(),
            "to",
            trackingInfo
        ));
    }
}
