/*
 * (c) Copyright 2015-2016 Hewlett Packard Enterprise Development LP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hpe.caf.util.rabbitmq;


import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.internal.junit.ArrayAsserts;

import com.rabbitmq.client.Envelope;


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
        ArrayAsserts.assertArrayEquals(body, impl.getLastDelivery().getMessageData());
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
