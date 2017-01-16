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

import com.rabbitmq.client.Channel;


public class DefaultRabbitPublisherTest
{
    private static final byte[] DATA = "data".getBytes(StandardCharsets.UTF_8);


    @Test
    public void testHandlePublish()
        throws InterruptedException
    {
        BlockingQueue<Event<QueuePublisher>> events = new LinkedBlockingQueue<>();
        Channel ch = Mockito.mock(Channel.class);
        CountDownLatch latch = new CountDownLatch(1);
        TestQueuePublisherImpl impl = new TestQueuePublisherImpl(latch);
        DefaultRabbitPublisher pub = new DefaultRabbitPublisher(events, impl);
        Thread t = new Thread(pub);
        t.start();
        events.offer(new PublisherPublishEvent(DATA));
        Assert.assertTrue(latch.await(DefaultRabbitPublisher.POLL_PERIOD, TimeUnit.MILLISECONDS));
        ArrayAsserts.assertArrayEquals(DATA, impl.getLastData());
        pub.shutdown();
    }


    @Test
    public void testHandleShutdown()
        throws InterruptedException
    {
        BlockingQueue<Event<QueuePublisher>> events = new LinkedBlockingQueue<>();
        Channel ch = Mockito.mock(Channel.class);
        CountDownLatch latch = new CountDownLatch(1);
        TestQueuePublisherImpl impl = new TestQueuePublisherImpl(latch);
        DefaultRabbitPublisher pub = new DefaultRabbitPublisher(events, impl);
        Thread t = new Thread(pub);
        t.start();
        pub.shutdown();
        events.offer(new PublisherPublishEvent(DATA));
        Assert.assertFalse(latch.await(DefaultRabbitPublisher.POLL_PERIOD, TimeUnit.MILLISECONDS));
    }


    private static class TestQueuePublisherImpl implements QueuePublisher
    {
        private final CountDownLatch latch;
        private byte[] lastData;


        public TestQueuePublisherImpl(final CountDownLatch latch)
        {
            this.latch = Objects.requireNonNull(latch);
        }


        @Override
        public void handlePublish(final byte[] data)
        {
            lastData = data;
            latch.countDown();
        }


        public byte[] getLastData()
        {
            return lastData;
        }
    }
}
