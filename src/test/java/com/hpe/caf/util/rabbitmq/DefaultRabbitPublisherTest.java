package com.hpe.caf.util.rabbitmq;


import com.rabbitmq.client.Channel;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;


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
        Assert.assertArrayEquals(DATA, impl.getLastData());
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
