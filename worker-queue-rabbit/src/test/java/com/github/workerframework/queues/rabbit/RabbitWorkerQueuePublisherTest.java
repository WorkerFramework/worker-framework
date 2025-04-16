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
import com.github.workerframework.api.ManagedDataStore;
import com.github.workerframework.api.ReferenceNotFoundException;
import com.github.workerframework.api.TaskMessage;
import com.github.workerframework.api.TaskStatus;
import com.github.workerframework.api.TrackingInfo;
import com.github.workerframework.configs.MessageDehydrationConfiguration;
import com.github.workerframework.datastores.fs.FileSystemDataStore;
import com.github.workerframework.datastores.fs.FileSystemDataStoreConfiguration;
import com.github.workerframework.util.rabbitmq.ConsumerRejectEvent;
import com.github.workerframework.util.rabbitmq.Event;
import com.github.workerframework.util.rabbitmq.EventPoller;
import com.github.workerframework.util.rabbitmq.QueueConsumer;
import com.rabbitmq.client.Channel;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.when;

public class RabbitWorkerQueuePublisherTest
{
    private String testQueue = "testQueue";
    private RabbitTaskInformation taskInformation;
    private byte[] data = "test123".getBytes(StandardCharsets.UTF_8);
    private RabbitMetricsReporter metrics = new RabbitMetricsReporter();

    private File tempDataStore;
    private ManagedDataStore dataStore;
    private static Codec codec;
    private static RabbitWorkerQueueConfiguration config;

    @BeforeClass
    public static void beforeClass() {
        codec = new JsonCodec();
        config = Mockito.mock(RabbitWorkerQueueConfiguration.class);
        when(config.getMessageDehydrationConfig()).thenReturn(new MessageDehydrationConfiguration());
    }

    @BeforeMethod
    public void beforeMethod() throws DataStoreException {
        taskInformation = new RabbitTaskInformation("101");
        tempDataStore = new File("RabbitWorkerQueuePublisherTest");
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
    public void testWorkerLoadsTheStoredMessageProcessesItThenDeletesItFromTheStore()
        throws InterruptedException, IOException, CodecException, DataStoreException {

        final var trackingInfo = new TrackingInfo("task1", new Date(), 1, "hello.com", "pipe", "to");

        final var actualTaskData = "This is the actual task message that gets stored";
        final var storedTaskMessage = new TaskMessage(
            "task1",
            "ACTUAL_CLASSIFIER",
            1,
            actualTaskData.getBytes(StandardCharsets.UTF_8),
            TaskStatus.NEW_TASK,
            new HashMap<>(),
            "to",
            trackingInfo);
        final var storedTaskMessageData = codec.serialise(storedTaskMessage);
        final var dehydratedMessageId = dataStore.store(storedTaskMessageData, "testQueue/task1");

        //  WorkerCore would set this to allow the publisher to delete.
        taskInformation.setRehydratedMessageId(dehydratedMessageId);

        final var taskMessage = new TaskMessage(
            "task1",
            WorkerPublisherImpl.DEHYDRATED_MESSAGE_TASK_NAME,
            1,
            dehydratedMessageId.getBytes(StandardCharsets.UTF_8),
            TaskStatus.NEW_TASK,
            new HashMap<>(),
            "to",
            trackingInfo);
        final var taskMessageData = codec.serialise(taskMessage);

        final RabbitWorkerQueueConfiguration dehydrationEnabledCfg = Mockito.mock(RabbitWorkerQueueConfiguration.class);
        final MessageDehydrationConfiguration dehydrationConfiguration = new MessageDehydrationConfiguration();
        dehydrationConfiguration.setEnabled(true);
        dehydrationConfiguration.setThreshold(1);
        when(dehydrationEnabledCfg.getMessageDehydrationConfig()).thenReturn(dehydrationConfiguration);

        BlockingQueue<Event<QueueConsumer>> consumerEvents = new LinkedBlockingQueue<>();
        BlockingQueue<Event<WorkerPublisher>> publisherEvents = new LinkedBlockingQueue<>();
        Channel channel = Mockito.mock(Channel.class);
        CountDownLatch latch = new CountDownLatch(1);
        Answer<Void> a = invocationOnMock -> {
            latch.countDown();
            return null;
        };
        Mockito.doAnswer(a).when(channel).basicPublish(Mockito.any(), Mockito.eq(testQueue), Mockito.any(), Mockito.eq(data));
        WorkerConfirmListener listener = Mockito.mock(WorkerConfirmListener.class);
        WorkerPublisher impl = new WorkerPublisherImpl(channel, metrics, consumerEvents, listener, dataStore, dehydrationEnabledCfg, codec);
        EventPoller<WorkerPublisher> publisher = new EventPoller<>(2, publisherEvents, impl);
        Thread t = new Thread(publisher);
        t.start();
        publisherEvents.add(new WorkerPublishQueueEvent(taskMessageData, testQueue, taskInformation));
        latch.await(5000, TimeUnit.MILLISECONDS);
        publisher.shutdown();

        Assert.assertThrows(
            "The stored message should have been deleted",
            ReferenceNotFoundException.class,
            () -> dataStore.retrieve(dehydratedMessageId)
        );

        Assert.assertEquals(0, publisherEvents.size());
        Assert.assertEquals(0, consumerEvents.size());


    }

    @Test
    public void testSetup()
        throws IOException
    {
        BlockingQueue<Event<QueueConsumer>> consumerEvents = new LinkedBlockingQueue<>();
        Channel channel = Mockito.mock(Channel.class);
        WorkerConfirmListener listener = Mockito.mock(WorkerConfirmListener.class);
        WorkerPublisher impl = new WorkerPublisherImpl(channel, metrics, consumerEvents, listener, dataStore, config, codec);
        Mockito.verify(channel, Mockito.times(1)).confirmSelect();
        Mockito.verify(channel, Mockito.times(1)).addConfirmListener(listener);
    }

    @Test
    public void testHandlePublish()
        throws IOException, InterruptedException
    {
        BlockingQueue<Event<QueueConsumer>> consumerEvents = new LinkedBlockingQueue<>();
        BlockingQueue<Event<WorkerPublisher>> publisherEvents = new LinkedBlockingQueue<>();
        Channel channel = Mockito.mock(Channel.class);
        CountDownLatch latch = new CountDownLatch(1);
        Answer<Void> a = invocationOnMock -> {
            latch.countDown();
            return null;
        };
        Mockito.doAnswer(a).when(channel).basicPublish(Mockito.any(), Mockito.eq(testQueue), Mockito.any(), Mockito.eq(data));
        WorkerConfirmListener listener = Mockito.mock(WorkerConfirmListener.class);
        WorkerPublisher impl = new WorkerPublisherImpl(channel, metrics, consumerEvents, listener, dataStore, config, codec);
        EventPoller<WorkerPublisher> publisher = new EventPoller<>(2, publisherEvents, impl);
        Thread t = new Thread(publisher);
        t.start();
        publisherEvents.add(new WorkerPublishQueueEvent(data, testQueue, taskInformation));
        latch.await(5000, TimeUnit.MILLISECONDS);
        publisher.shutdown();
        Assert.assertEquals(0, publisherEvents.size());
        Assert.assertEquals(0, consumerEvents.size());
    }

    @Test
    public void testHandlePublishFail()
        throws IOException, InterruptedException
    {
        BlockingQueue<Event<QueueConsumer>> consumerEvents = new LinkedBlockingQueue<>();
        BlockingQueue<Event<WorkerPublisher>> publisherEvents = new LinkedBlockingQueue<>();
        Channel channel = Mockito.mock(Channel.class);
        WorkerConfirmListener listener = Mockito.mock(WorkerConfirmListener.class);
        Mockito.doThrow(IOException.class).when(channel).basicPublish(Mockito.any(), Mockito.eq(testQueue), Mockito.any(), Mockito.eq(data));
        WorkerPublisher impl = new WorkerPublisherImpl(channel, metrics, consumerEvents, listener, dataStore, config, codec);
        EventPoller<WorkerPublisher> publisher = new EventPoller<>(2, publisherEvents, impl);
        Thread t = new Thread(publisher);
        t.start();
        publisherEvents.add(new WorkerPublishQueueEvent(data, testQueue, taskInformation));
        Event<QueueConsumer> event = consumerEvents.poll(5000, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(event);
        Assert.assertTrue(event instanceof ConsumerRejectEvent);
        Assert.assertEquals((long)Long.valueOf(taskInformation.getInboundMessageId()), ((ConsumerRejectEvent) event).getTag());
        Mockito.verify(channel, Mockito.times(1)).basicPublish(Mockito.any(), Mockito.eq(testQueue), Mockito.any(), Mockito.eq(data));
        publisher.shutdown();
        Assert.assertEquals(0, publisherEvents.size());
        Assert.assertEquals(0, consumerEvents.size());
    }
}
