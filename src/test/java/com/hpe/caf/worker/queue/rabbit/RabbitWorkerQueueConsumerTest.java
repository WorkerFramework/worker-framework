package com.hpe.caf.worker.queue.rabbit;


import com.hpe.caf.api.worker.NewTaskCallback;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.util.rabbitmq.ConsumerEventType;
import com.hpe.caf.util.rabbitmq.ConsumerQueueEvent;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


@RunWith(MockitoJUnitRunner.class)
public class RabbitWorkerQueueConsumerTest
{
    private String testQueue = "testQueue";
    private long id = 101L;
    private byte[] data = "test123".getBytes(StandardCharsets.UTF_8);
    private Envelope env = new Envelope(id, false, "", testQueue);
    private RabbitMetricsReporter metrics = new RabbitMetricsReporter();
    @Mock
    private LinkedBlockingQueue<QueueingConsumer.Delivery> mockDeliveryQueue;
    @Mock
    private NewTaskCallback mockCallback;


    @Test
    public void testHandleDelivery()
            throws IOException, InterruptedException, WorkerException
    {
        BlockingQueue<ConsumerQueueEvent> consumerEvents = new LinkedBlockingQueue<>();
        BlockingQueue<QueueingConsumer.Delivery> deliveries = new LinkedBlockingQueue<>();
        QueueingConsumer.Delivery del = new QueueingConsumer.Delivery(env, null, data);
        Channel channel = Mockito.mock(Channel.class);
        CountDownLatch latch = new CountDownLatch(1);
        NewTaskCallback callback = new TestCallback(latch, false);
        RabbitWorkerQueueConsumer consumer = new RabbitWorkerQueueConsumer(deliveries, consumerEvents, channel, callback, metrics);
        Thread t = new Thread(consumer);
        t.start();
        deliveries.add(del);
        consumer.handleDelivery("consumer", env, null, data);
        Assert.assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
        consumer.shutdown();
    }


    @Test
    public void testHandleDeliveryFail()
            throws IOException, InterruptedException, WorkerException
    {
        BlockingQueue<ConsumerQueueEvent> consumerEvents = new LinkedBlockingQueue<>();
        BlockingQueue<QueueingConsumer.Delivery> deliveries = new LinkedBlockingQueue<>();
        CountDownLatch deliverLatch = new CountDownLatch(1);
        CountDownLatch channelLatch = new CountDownLatch(1);
        QueueingConsumer.Delivery del = new QueueingConsumer.Delivery(env, null, data);
        Channel channel = Mockito.mock(Channel.class);
        Answer<Void> a = invocationOnMock -> {
            channelLatch.countDown();
            return null;
        };
        Mockito.doAnswer(a).when(channel).basicReject(Mockito.eq(id), Mockito.anyBoolean());
        NewTaskCallback callback = new TestCallback(deliverLatch, true);
        RabbitWorkerQueueConsumer consumer = new RabbitWorkerQueueConsumer(deliveries, consumerEvents, channel, callback, metrics);
        Thread t = new Thread(consumer);
        t.start();
        deliveries.add(del);
        consumer.handleDelivery("consumer", env, null, data);
        Assert.assertTrue(deliverLatch.await(1000, TimeUnit.MILLISECONDS));
        Assert.assertTrue(channelLatch.await(1000, TimeUnit.MILLISECONDS));
        consumer.shutdown();
    }


    @Test
    public void testHandleDeliveryAck()
            throws IOException, InterruptedException, WorkerException
    {
        BlockingQueue<ConsumerQueueEvent> consumerEvents = new LinkedBlockingQueue<>();
        CountDownLatch channelLatch = new CountDownLatch(1);
        Channel channel = Mockito.mock(Channel.class);
        Answer<Void> a = invocationOnMock -> {
            channelLatch.countDown();
            return null;
        };
        Mockito.doAnswer(a).when(channel).basicAck(Mockito.eq(id), Mockito.anyBoolean());
        RabbitWorkerQueueConsumer consumer =
                new RabbitWorkerQueueConsumer(mockDeliveryQueue, consumerEvents, channel, mockCallback, metrics);
        Thread t = new Thread(consumer);
        t.start();
        consumerEvents.add(new ConsumerQueueEvent(ConsumerEventType.ACK, id));
        Assert.assertTrue(channelLatch.await(30000, TimeUnit.MILLISECONDS));
        consumer.shutdown();
    }


    @Test
    public void testHandleDeliveryReject()
            throws IOException, InterruptedException, WorkerException
    {
        BlockingQueue<ConsumerQueueEvent> consumerEvents = new LinkedBlockingQueue<>();
        CountDownLatch channelLatch = new CountDownLatch(1);
        Channel channel = Mockito.mock(Channel.class);
        Answer<Void> a = invocationOnMock -> {
            channelLatch.countDown();
            return null;
        };
        Mockito.doAnswer(a).when(channel).basicReject(Mockito.eq(id), Mockito.eq(true));
        RabbitWorkerQueueConsumer consumer = new RabbitWorkerQueueConsumer(mockDeliveryQueue, consumerEvents, channel, mockCallback, metrics);
        Thread t = new Thread(consumer);
        t.start();
        consumerEvents.add(new ConsumerQueueEvent(ConsumerEventType.REJECT, id));
        Assert.assertTrue(channelLatch.await(1000, TimeUnit.MILLISECONDS));
        consumer.shutdown();
    }


    @Test
    public void testHandleDeliveryDrop()
            throws IOException, InterruptedException, WorkerException
    {
        BlockingQueue<ConsumerQueueEvent> consumerEvents = new LinkedBlockingQueue<>();
        CountDownLatch channelLatch = new CountDownLatch(1);
        Channel channel = Mockito.mock(Channel.class);
        Answer<Void> a = invocationOnMock -> {
            channelLatch.countDown();
            return null;
        };
        Mockito.doAnswer(a).when(channel).basicReject(id, false);
        RabbitWorkerQueueConsumer consumer = new RabbitWorkerQueueConsumer(mockDeliveryQueue, consumerEvents, channel, mockCallback, metrics);
        Thread t = new Thread(consumer);
        t.start();
        consumerEvents.add(new ConsumerQueueEvent(ConsumerEventType.DROP, id));
        Assert.assertTrue(channelLatch.await(1000, TimeUnit.MILLISECONDS));
        consumer.shutdown();
    }


    private class TestCallback implements NewTaskCallback
    {
        private final CountDownLatch latch;
        private final boolean throwException;


        public TestCallback(final CountDownLatch latch, final boolean throwException)
        {
            this.latch = latch;
            this.throwException = throwException;
        }


        @Override
        public void registerNewTask(final String taskId, final byte[] taskData)
                throws WorkerException
        {
            latch.countDown();
            if ( throwException ) {
                throw new WorkerException("test exception");
            }
        }
    }

}
