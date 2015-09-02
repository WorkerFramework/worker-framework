package com.hpe.caf.util.rabbitmq;


import com.hpe.caf.util.rabbitmq.ConsumerEventType;
import com.hpe.caf.util.rabbitmq.ConsumerQueueEvent;
import com.hpe.caf.util.rabbitmq.RabbitConsumer;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.QueueingConsumer;
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


public class RabbitConsumerTest
{
    @Test
    public void testProcessDelivery()
        throws InterruptedException, IOException
    {
        BlockingQueue<QueueingConsumer.Delivery> q = new LinkedBlockingQueue<>();
        BlockingQueue<ConsumerQueueEvent> events = new LinkedBlockingQueue<>();
        CountDownLatch latch = new CountDownLatch(1);
        TestRabbitConsumer con = new TestRabbitConsumer(q, events, Mockito.mock(Channel.class), latch);
        new Thread(con).start();
        long tag = 100L;
        byte[] body = "data".getBytes(StandardCharsets.UTF_8);
        con.handleDelivery(String.valueOf(tag), Mockito.mock(Envelope.class), Mockito.mock(AMQP.BasicProperties.class), body);
        events.offer(new ConsumerQueueEvent(ConsumerEventType.DELIVER, tag));
        Assert.assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
        Assert.assertArrayEquals(body, con.getLastDelivery().getBody());
    }


    @Test
    public void testProcessAck()
        throws InterruptedException
    {
        BlockingQueue<QueueingConsumer.Delivery> q = new LinkedBlockingQueue<>();
        BlockingQueue<ConsumerQueueEvent> events = new LinkedBlockingQueue<>();
        CountDownLatch latch = new CountDownLatch(1);
        TestRabbitConsumer con = new TestRabbitConsumer(q, events, Mockito.mock(Channel.class), latch);
        new Thread(con).start();
        long tag = 100L;
        events.offer(new ConsumerQueueEvent(ConsumerEventType.ACK, tag));
        Assert.assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
        Assert.assertEquals(tag, con.getLastTag());
    }


    @Test
    public void testProcessReject()
        throws InterruptedException
    {
        BlockingQueue<QueueingConsumer.Delivery> q = new LinkedBlockingQueue<>();
        BlockingQueue<ConsumerQueueEvent> events = new LinkedBlockingQueue<>();
        CountDownLatch latch = new CountDownLatch(1);
        TestRabbitConsumer con = new TestRabbitConsumer(q, events, Mockito.mock(Channel.class), latch);
        new Thread(con).start();
        long tag = 100L;
        events.offer(new ConsumerQueueEvent(ConsumerEventType.REJECT, tag));
        Assert.assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
        Assert.assertEquals(tag, con.getLastTag());
    }


    @Test
    public void testProcessDrop()
        throws InterruptedException
    {
        BlockingQueue<QueueingConsumer.Delivery> q = new LinkedBlockingQueue<>();
        BlockingQueue<ConsumerQueueEvent> events = new LinkedBlockingQueue<>();
        CountDownLatch latch = new CountDownLatch(1);
        TestRabbitConsumer con = new TestRabbitConsumer(q, events, Mockito.mock(Channel.class), latch);
        new Thread(con).start();
        long tag = 100L;
        events.offer(new ConsumerQueueEvent(ConsumerEventType.DROP, tag));
        Assert.assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
        Assert.assertEquals(tag, con.getLastTag());
    }


    @Test
    public void testHandleShutdown()
        throws InterruptedException
    {
        BlockingQueue<QueueingConsumer.Delivery> q = new LinkedBlockingQueue<>();
        BlockingQueue<ConsumerQueueEvent> events = new LinkedBlockingQueue<>();
        CountDownLatch latch = new CountDownLatch(1);
        TestRabbitConsumer con = new TestRabbitConsumer(q, events, Mockito.mock(Channel.class), latch);
        new Thread(con).start();
        long tag = 100L;
        con.shutdown();
        events.offer(new ConsumerQueueEvent(ConsumerEventType.DROP, tag));
        Assert.assertFalse(latch.await(1000, TimeUnit.MILLISECONDS));
    }


    private static class TestRabbitConsumer extends RabbitConsumer<ConsumerQueueEvent>
    {
        private final CountDownLatch latch;
        private Delivery lastDelivery;
        private long lastTag;


        public TestRabbitConsumer(final BlockingQueue<Delivery> q, final BlockingQueue<ConsumerQueueEvent> events, final Channel channel, final CountDownLatch latch)
        {
            super(q, events, channel);
            this.latch = Objects.requireNonNull(latch);
        }


        @Override
        protected ConsumerQueueEvent getDeliverEvent(final long tag)
        {
            return new ConsumerQueueEvent(ConsumerEventType.DELIVER, tag);
        }


        @Override
        protected void processDelivery(final Delivery delivery, final ConsumerQueueEvent event)
        {
            lastDelivery = delivery;
            latch.countDown();
        }


        @Override
        protected void processAck(final ConsumerQueueEvent event)
        {
            lastTag = event.getMessageTag();
            latch.countDown();
        }


        @Override
        protected void processReject(final ConsumerQueueEvent event)
        {
            lastTag = event.getMessageTag();
            latch.countDown();
        }


        @Override
        protected void processDrop(final ConsumerQueueEvent event)
        {
            lastTag = event.getMessageTag();
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
