package com.hp.caf.util.rabbitmq;


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


public class RabbitPublisherTest
{
    private static final long TAG = 1L;
    private static final byte[] DATA = "data".getBytes(StandardCharsets.UTF_8);
    private static final String QUEUE = "queue";


    @Test
    public void testHandlePublish()
        throws InterruptedException
    {
        BlockingQueue<PublishQueueEvent> events = new LinkedBlockingQueue<>();
        Channel ch = Mockito.mock(Channel.class);
        CountDownLatch latch = new CountDownLatch(1);
        TestRabbitPublisher pub = new TestRabbitPublisher(events, ch, latch);
        Thread t = new Thread(pub);
        t.start();
        events.offer(new PublishQueueEvent(PublishEventType.PUBLISH, TAG, DATA, QUEUE));
        Assert.assertTrue(latch.await(1000, TimeUnit.MILLISECONDS));
        Assert.assertEquals(TAG, pub.getLastTag());
        Assert.assertEquals(QUEUE, pub.getLastQueue());
        Assert.assertArrayEquals(DATA, pub.getLastData());
        pub.shutdown();
    }


    @Test
    public void testHandleShutdown()
        throws InterruptedException
    {
        BlockingQueue<PublishQueueEvent> events = new LinkedBlockingQueue<>();
        Channel ch = Mockito.mock(Channel.class);
        CountDownLatch latch = new CountDownLatch(1);
        TestRabbitPublisher pub = new TestRabbitPublisher(events, ch, latch);
        Thread t = new Thread(pub);
        t.start();
        pub.shutdown();
        events.offer(new PublishQueueEvent(PublishEventType.PUBLISH, TAG, DATA, QUEUE));
        Assert.assertFalse(latch.await(1000, TimeUnit.MILLISECONDS));
    }


    private static class TestRabbitPublisher extends RabbitPublisher<PublishQueueEvent>
    {
        private final CountDownLatch latch;
        private long lastTag;
        private byte[] lastData;
        private String lastQueue;


        public TestRabbitPublisher(final BlockingQueue<PublishQueueEvent> events, final Channel channel, final CountDownLatch latch)
        {
            super(events, channel);
            this.latch = Objects.requireNonNull(latch);
        }


        public long getLastTag()
        {
            return lastTag;
        }


        public byte[] getLastData()
        {
            return lastData;
        }


        public String getLastQueue()
        {
            return lastQueue;
        }


        @Override
        protected void handlePublish(final PublishQueueEvent event)
        {
            lastTag = event.getMessageTag();
            lastData = event.getEventData();
            lastQueue = event.getQueue();
            latch.countDown();
        }
    }
}
