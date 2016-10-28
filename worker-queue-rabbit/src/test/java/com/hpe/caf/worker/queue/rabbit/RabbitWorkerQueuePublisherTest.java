package com.hpe.caf.worker.queue.rabbit;


import com.hpe.caf.util.rabbitmq.ConsumerRejectEvent;
import com.hpe.caf.util.rabbitmq.Event;
import com.hpe.caf.util.rabbitmq.EventPoller;
import com.hpe.caf.util.rabbitmq.QueueConsumer;
import com.rabbitmq.client.Channel;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


public class RabbitWorkerQueuePublisherTest
{
    private String testQueue = "testQueue";
    private long id = 101L;
    private byte[] data = "test123".getBytes(StandardCharsets.UTF_8);
    private RabbitMetricsReporter metrics = new RabbitMetricsReporter();


    @Test
    public void testSetup()
        throws IOException
    {
        BlockingQueue<Event<QueueConsumer>> consumerEvents = new LinkedBlockingQueue<>();
        Channel channel = Mockito.mock(Channel.class);
        WorkerConfirmListener listener = Mockito.mock(WorkerConfirmListener.class);
        WorkerPublisher impl = new WorkerPublisherImpl(channel, metrics, consumerEvents, listener);
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
        WorkerConfirmListener listener = Mockito.mock(WorkerConfirmListener.class);
        WorkerPublisher impl = new WorkerPublisherImpl(channel, metrics, consumerEvents, listener);
        EventPoller<WorkerPublisher> publisher = new EventPoller<>(2, publisherEvents, impl);
        Thread t = new Thread(publisher);
        t.start();
        publisherEvents.add(new WorkerPublishQueueEvent(data, testQueue, id));
        CountDownLatch latch = new CountDownLatch(1);
        Answer<Void> a = invocationOnMock -> {
            latch.countDown();
            return null;
        };
        Mockito.doAnswer(a).when(channel).basicPublish(Mockito.any(), Mockito.eq(testQueue), Mockito.any(), Mockito.eq(data));
        latch.await(1000, TimeUnit.MILLISECONDS);
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
        WorkerPublisher impl = new WorkerPublisherImpl(channel, metrics, consumerEvents, listener);
        EventPoller<WorkerPublisher> publisher = new EventPoller<>(2, publisherEvents, impl);
        Thread t = new Thread(publisher);
        t.start();
        publisherEvents.add(new WorkerPublishQueueEvent(data, testQueue, id));
        Event<QueueConsumer> event = consumerEvents.poll(5000, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(event);
        Assert.assertTrue(event instanceof ConsumerRejectEvent);
        Assert.assertEquals(id, ((ConsumerRejectEvent) event).getTag());
        Mockito.verify(channel, Mockito.times(1)).basicPublish(Mockito.any(), Mockito.eq(testQueue), Mockito.any(), Mockito.eq(data));
        publisher.shutdown();
        Assert.assertEquals(0, publisherEvents.size());
        Assert.assertEquals(0, consumerEvents.size());
    }
}
