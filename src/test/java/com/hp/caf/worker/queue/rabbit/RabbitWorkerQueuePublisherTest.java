package com.hp.caf.worker.queue.rabbit;


import com.hp.caf.util.rabbitmq.ConsumerEventType;
import com.hp.caf.util.rabbitmq.ConsumerQueueEvent;
import com.hp.caf.util.rabbitmq.PublishEventType;
import com.hp.caf.util.rabbitmq.PublishQueueEvent;
import com.rabbitmq.client.Channel;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


public class RabbitWorkerQueuePublisherTest
{
    private String testQueue = "testQueue";
    private long id = 101L;
    private byte[] data = "test123".getBytes(StandardCharsets.UTF_8);
    private RabbitMetricsReporter metrics = new RabbitMetricsReporter();


    @Test
    public void testHandlePublish()
            throws IOException, InterruptedException
    {
        BlockingQueue<ConsumerQueueEvent> consumerEvents = new LinkedBlockingQueue<>();
        BlockingQueue<PublishQueueEvent> publisherEvents = new LinkedBlockingQueue<>();
        Channel channel = Mockito.mock(Channel.class);
        RabbitWorkerQueuePublisher publisher = new RabbitWorkerQueuePublisher(publisherEvents, consumerEvents, channel, metrics);
        Thread t = new Thread(publisher);
        t.start();
        publisherEvents.add(new PublishQueueEvent(PublishEventType.PUBLISH, id, data, testQueue));
        ConsumerQueueEvent event = consumerEvents.poll(5000, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(event);
        Assert.assertEquals(ConsumerEventType.ACK, event.getEventType());
        Assert.assertEquals(id, event.getMessageTag());
        Mockito.verify(channel, Mockito.times(1)).basicPublish(Mockito.any(), Mockito.eq(testQueue), Mockito.any(), Mockito.eq(data));
        publisher.shutdown();
        Assert.assertEquals(0, publisherEvents.size());
        Assert.assertEquals(0, consumerEvents.size());
    }


    @Test
    public void testHandlePublishFail()
            throws IOException, InterruptedException
    {
        BlockingQueue<ConsumerQueueEvent> consumerEvents = new LinkedBlockingQueue<>();
        BlockingQueue<PublishQueueEvent> publisherEvents = new LinkedBlockingQueue<>();
        Channel channel = Mockito.mock(Channel.class);
        Mockito.doThrow(IOException.class).when(channel).basicPublish(Mockito.any(), Mockito.eq(testQueue), Mockito.any(), Mockito.eq(data));
        RabbitWorkerQueuePublisher publisher = new RabbitWorkerQueuePublisher(publisherEvents, consumerEvents, channel, metrics);
        Thread t = new Thread(publisher);
        t.start();
        publisherEvents.add(new PublishQueueEvent(PublishEventType.PUBLISH, id, data, testQueue));
        ConsumerQueueEvent event = consumerEvents.poll(5000, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(event);
        Assert.assertEquals(ConsumerEventType.REJECT, event.getEventType());
        Assert.assertEquals(id, event.getMessageTag());
        Mockito.verify(channel, Mockito.times(1)).basicPublish(Mockito.any(), Mockito.eq(testQueue), Mockito.any(), Mockito.eq(data));
        publisher.shutdown();
        Assert.assertEquals(0, publisherEvents.size());
        Assert.assertEquals(0, consumerEvents.size());
    }
}
