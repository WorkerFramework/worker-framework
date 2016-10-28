package com.hpe.caf.util.rabbitmq;


import com.rabbitmq.client.Envelope;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


public class DefaultRabbitConsumerTest
{
    @Test
    public void testProcessDelivery()
        throws InterruptedException, IOException
    {
        BlockingQueue<Event<QueueConsumer>> events = new LinkedBlockingQueue<>();
        CountDownLatch latch = new CountDownLatch(1);
        TestQueueConsumerImpl impl = new TestQueueConsumerImpl(latch);
        DefaultRabbitConsumer con = new DefaultRabbitConsumer(events, impl);
        new Thread(con).start();
        long tag = 100L;
        byte[] body = "data".getBytes(StandardCharsets.UTF_8);
        Envelope env = Mockito.mock(Envelope.class);
        Mockito.when(env.getDeliveryTag()).thenReturn(tag);
        events.offer(new ConsumerDeliverEvent(new Delivery(env, body)));
        Assert.assertTrue(latch.await(DefaultRabbitConsumer.POLL_PERIOD, TimeUnit.MILLISECONDS));
        Assert.assertArrayEquals(body, impl.getLastDelivery().getMessageData());
    }


    @Test
    public void testProcessAck()
        throws InterruptedException
    {
        BlockingQueue<Event<QueueConsumer>> events = new LinkedBlockingQueue<>();
        CountDownLatch latch = new CountDownLatch(1);
        TestQueueConsumerImpl impl = new TestQueueConsumerImpl(latch);
        DefaultRabbitConsumer con = new DefaultRabbitConsumer(events, impl);
        new Thread(con).start();
        long tag = 100L;
        events.offer(new ConsumerAckEvent(tag));
        Assert.assertTrue(latch.await(DefaultRabbitConsumer.POLL_PERIOD, TimeUnit.MILLISECONDS));
        Assert.assertEquals(tag, impl.getLastTag());
    }


    @Test
    public void testProcessReject()
        throws InterruptedException
    {
        BlockingQueue<Event<QueueConsumer>> events = new LinkedBlockingQueue<>();
        CountDownLatch latch = new CountDownLatch(1);
        TestQueueConsumerImpl impl = new TestQueueConsumerImpl(latch);
        DefaultRabbitConsumer con = new DefaultRabbitConsumer(events, impl);
        new Thread(con).start();
        long tag = 100L;
        events.offer(new ConsumerRejectEvent(tag));
        Assert.assertTrue(latch.await(DefaultRabbitConsumer.POLL_PERIOD, TimeUnit.MILLISECONDS));
        Assert.assertEquals(tag, impl.getLastTag());
    }


    @Test
    public void testProcessDrop()
        throws InterruptedException
    {
        BlockingQueue<Event<QueueConsumer>> events = new LinkedBlockingQueue<>();
        CountDownLatch latch = new CountDownLatch(1);
        TestQueueConsumerImpl impl = new TestQueueConsumerImpl(latch);
        DefaultRabbitConsumer con = new DefaultRabbitConsumer(events, impl);
        new Thread(con).start();
        long tag = 100L;
        events.offer(new ConsumerDropEvent(tag));
        Assert.assertTrue(latch.await(DefaultRabbitConsumer.POLL_PERIOD, TimeUnit.MILLISECONDS));
        Assert.assertEquals(tag, impl.getLastTag());
    }


    @Test
    public void testHandleShutdown()
        throws InterruptedException
    {
        BlockingQueue<Event<QueueConsumer>> events = new LinkedBlockingQueue<>();
        CountDownLatch latch = new CountDownLatch(1);
        TestQueueConsumerImpl impl = new TestQueueConsumerImpl(latch);
        DefaultRabbitConsumer con = new DefaultRabbitConsumer(events, impl);
        new Thread(con).start();
        long tag = 100L;
        con.shutdown();
        events.offer(new ConsumerDropEvent(tag));
        Assert.assertFalse(latch.await(DefaultRabbitConsumer.POLL_PERIOD, TimeUnit.MILLISECONDS));
    }


    private static class TestQueueConsumerImpl implements QueueConsumer
    {
        private final CountDownLatch latch;
        private Delivery lastDelivery;
        private long lastTag;

        public TestQueueConsumerImpl(final CountDownLatch latch)
        {
            this.latch = Objects.requireNonNull(latch);
        }


        @Override
        public void processDelivery(Delivery delivery)
        {
            lastDelivery = delivery;
            latch.countDown();
        }


        @Override
        public void processAck(long tag)
        {
            lastTag = tag;
            latch.countDown();
        }


        @Override
        public void processReject(long tag)
        {
            lastTag = tag;
            latch.countDown();
        }


        @Override
        public void processDrop(long tag)
        {
            lastTag = tag;
            latch.countDown();
        }


        public Delivery getLastDelivery()
        {
            return lastDelivery;
        }


        public long getLastTag()
        {
            return lastTag;
        }
    }
}
