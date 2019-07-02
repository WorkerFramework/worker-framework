/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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
package com.hpe.caf.worker.queue.rabbit;

import com.hpe.caf.util.rabbitmq.ConsumerAckEvent;
import com.hpe.caf.util.rabbitmq.ConsumerRejectEvent;
import com.hpe.caf.util.rabbitmq.Event;
import com.hpe.caf.util.rabbitmq.QueueConsumer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class WorkerConfirmListenerTest
{
    @Test
    public void testAckSingle()
        throws IOException, InterruptedException
    {
        BlockingQueue<Event<QueueConsumer>> q = new LinkedBlockingQueue<>();
        WorkerConfirmListener conf = new WorkerConfirmListener(q);
        RabbitTaskInformation rabbitTaskInformation = new RabbitTaskInformation("100");
        rabbitTaskInformation.incrementResponseCount(true);
        conf.registerResponseSequence(1, rabbitTaskInformation);
        conf.handleAck(1, false);
        Event<QueueConsumer> e = q.poll(1000, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(e);
        Assert.assertTrue(e instanceof ConsumerAckEvent);
        Assert.assertEquals(100, ((ConsumerAckEvent) e).getTag());
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testAckSingleMissing()
        throws IOException, InterruptedException
    {
        BlockingQueue<Event<QueueConsumer>> q = new LinkedBlockingQueue<>();
        WorkerConfirmListener conf = new WorkerConfirmListener(q);
        conf.handleAck(1, false);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testAckSingleDuplicate()
        throws IOException, InterruptedException
    {
        BlockingQueue<Event<QueueConsumer>> q = new LinkedBlockingQueue<>();
        WorkerConfirmListener conf = new WorkerConfirmListener(q);
        RabbitTaskInformation rabbitTaskInformation = new RabbitTaskInformation("100");
        rabbitTaskInformation.incrementResponseCount(true);
        conf.registerResponseSequence(1, rabbitTaskInformation);
        conf.handleAck(1, false);
        Event<QueueConsumer> e = q.poll(1000, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(e);
        Assert.assertTrue(e instanceof ConsumerAckEvent);
        Assert.assertEquals(100, ((ConsumerAckEvent) e).getTag());
        conf.handleAck(1, false);
    }

    @Test
    public void testNackSingle()
        throws IOException, InterruptedException
    {
        BlockingQueue<Event<QueueConsumer>> q = new LinkedBlockingQueue<>();
        WorkerConfirmListener conf = new WorkerConfirmListener(q);
        conf.registerResponseSequence(1, new RabbitTaskInformation("100"));
        conf.handleNack(1, false);
        Event<QueueConsumer> e = q.poll(1000, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(e);
        Assert.assertTrue(e instanceof ConsumerRejectEvent);
        Assert.assertEquals(100, ((ConsumerRejectEvent) e).getTag());
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testNackSingleMissing()
        throws IOException, InterruptedException
    {
        BlockingQueue<Event<QueueConsumer>> q = new LinkedBlockingQueue<>();
        WorkerConfirmListener conf = new WorkerConfirmListener(q);
        conf.handleNack(1, false);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testNackSingleDuplicate()
        throws IOException, InterruptedException
    {
        BlockingQueue<Event<QueueConsumer>> q = new LinkedBlockingQueue<>();
        WorkerConfirmListener conf = new WorkerConfirmListener(q);
        conf.registerResponseSequence(1, new RabbitTaskInformation("100"));
        conf.handleNack(1, false);
        Event<QueueConsumer> e = q.poll(1000, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(e);
        Assert.assertTrue(e instanceof ConsumerRejectEvent);
        Assert.assertEquals(100, ((ConsumerRejectEvent) e).getTag());
        conf.handleNack(1, false);
    }

    @Test
    public void testAckMultiple()
        throws IOException, InterruptedException
    {
        BlockingQueue<Event<QueueConsumer>> q = new LinkedBlockingQueue<>();
        WorkerConfirmListener conf = new WorkerConfirmListener(q);
        RabbitTaskInformation rabbitTaskInfo_100 = new RabbitTaskInformation("100");
        RabbitTaskInformation rabbitTaskInfo_500 = new RabbitTaskInformation("500");
        RabbitTaskInformation rabbitTaskInfo_200 = new RabbitTaskInformation("200");
        rabbitTaskInfo_100.incrementResponseCount(true);
        conf.registerResponseSequence(5, rabbitTaskInfo_500);
        rabbitTaskInfo_500.incrementResponseCount(true);
        conf.registerResponseSequence(1,rabbitTaskInfo_100);
        rabbitTaskInfo_200.incrementResponseCount(true);
        conf.registerResponseSequence(2, rabbitTaskInfo_200);
        conf.handleAck(4, true);
        Event<QueueConsumer> e = q.poll(1000, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(e);
        Assert.assertTrue(e instanceof ConsumerAckEvent);
        Assert.assertEquals(100L, ((ConsumerAckEvent) e).getTag());
        Event<QueueConsumer> e2 = q.poll(1000, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(e2);
        Assert.assertTrue(e2 instanceof ConsumerAckEvent);
        Assert.assertEquals(200L, ((ConsumerAckEvent) e2).getTag());
        Event<QueueConsumer> e3 = q.poll(1000, TimeUnit.MILLISECONDS);
        Assert.assertNull(e3);
        conf.handleAck(5, true);
        Event<QueueConsumer> e4 = q.poll(1000, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(e4);
        Assert.assertTrue(e4 instanceof ConsumerAckEvent);
        Assert.assertEquals(500L, ((ConsumerAckEvent) e4).getTag());
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testAckMultipleDuplicate()
        throws IOException, InterruptedException
    {
        BlockingQueue<Event<QueueConsumer>> q = new LinkedBlockingQueue<>();
        WorkerConfirmListener conf = new WorkerConfirmListener(q);
        RabbitTaskInformation rabbitTaskInfo_100 = new RabbitTaskInformation("100");
        RabbitTaskInformation rabbitTaskInfo_500 = new RabbitTaskInformation("500");
        RabbitTaskInformation rabbitTaskInfo_200 = new RabbitTaskInformation("200");
        rabbitTaskInfo_100.incrementResponseCount(true);
        conf.registerResponseSequence(5, rabbitTaskInfo_500);
        rabbitTaskInfo_500.incrementResponseCount(true);
        conf.registerResponseSequence(1,rabbitTaskInfo_100);
        rabbitTaskInfo_200.incrementResponseCount(true);
        conf.registerResponseSequence(2, rabbitTaskInfo_200);
        conf.handleAck(4, true);
        Event<QueueConsumer> e = q.poll(1000, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(e);
        Assert.assertTrue(e instanceof ConsumerAckEvent);
        Assert.assertEquals(100L, ((ConsumerAckEvent) e).getTag());
        Event<QueueConsumer> e2 = q.poll(1000, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(e2);
        Assert.assertTrue(e2 instanceof ConsumerAckEvent);
        Assert.assertEquals(200L, ((ConsumerAckEvent) e2).getTag());
        conf.handleAck(2, false);
    }

    @Test
    public void testNackMultiple()
        throws IOException, InterruptedException
    {
        BlockingQueue<Event<QueueConsumer>> q = new LinkedBlockingQueue<>();
        WorkerConfirmListener conf = new WorkerConfirmListener(q);
        RabbitTaskInformation rabbitTaskInfo_100 = new RabbitTaskInformation("100");
        RabbitTaskInformation rabbitTaskInfo_500 = new RabbitTaskInformation("500");
        RabbitTaskInformation rabbitTaskInfo_200 = new RabbitTaskInformation("200");
        rabbitTaskInfo_100.incrementResponseCount(true);
        conf.registerResponseSequence(5, rabbitTaskInfo_500);
        rabbitTaskInfo_500.incrementResponseCount(true);
        conf.registerResponseSequence(1,rabbitTaskInfo_100);
        rabbitTaskInfo_200.incrementResponseCount(true);
        conf.registerResponseSequence(2, rabbitTaskInfo_200);
        conf.handleNack(4, true);
        Event<QueueConsumer> e = q.poll(1000, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(e);
        Assert.assertTrue(e instanceof ConsumerRejectEvent);
        Assert.assertEquals(100L, ((ConsumerRejectEvent) e).getTag());
        Event<QueueConsumer> e2 = q.poll(1000, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(e2);
        Assert.assertTrue(e2 instanceof ConsumerRejectEvent);
        Assert.assertEquals(200L, ((ConsumerRejectEvent) e2).getTag());
        Event<QueueConsumer> e3 = q.poll(1000, TimeUnit.MILLISECONDS);
        Assert.assertNull(e3);
        conf.handleNack(5, true);
        Event<QueueConsumer> e4 = q.poll(1000, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(e4);
        Assert.assertTrue(e4 instanceof ConsumerRejectEvent);
        Assert.assertEquals(500L, ((ConsumerRejectEvent) e4).getTag());
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testNackMultipleDuplicate()
        throws IOException, InterruptedException
    {
        BlockingQueue<Event<QueueConsumer>> q = new LinkedBlockingQueue<>();
        WorkerConfirmListener conf = new WorkerConfirmListener(q);
        RabbitTaskInformation rabbitTaskInfo_100 = new RabbitTaskInformation("100");
        RabbitTaskInformation rabbitTaskInfo_500 = new RabbitTaskInformation("500");
        RabbitTaskInformation rabbitTaskInfo_200 = new RabbitTaskInformation("200");
        rabbitTaskInfo_100.incrementResponseCount(true);
        conf.registerResponseSequence(5, rabbitTaskInfo_500);
        rabbitTaskInfo_500.incrementResponseCount(true);
        conf.registerResponseSequence(1,rabbitTaskInfo_100);
        rabbitTaskInfo_200.incrementResponseCount(true);
        conf.registerResponseSequence(2, rabbitTaskInfo_200);
        conf.handleNack(4, true);
        Event<QueueConsumer> e = q.poll(1000, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(e);
        Assert.assertTrue(e instanceof ConsumerRejectEvent);
        Assert.assertEquals(100L, ((ConsumerRejectEvent) e).getTag());
        Event<QueueConsumer> e2 = q.poll(1000, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(e2);
        Assert.assertTrue(e2 instanceof ConsumerRejectEvent);
        Assert.assertEquals(200L, ((ConsumerRejectEvent) e2).getTag());
        conf.handleNack(2, false);
    }

    @Test
    public void testClearMap()
        throws IOException, InterruptedException
    {
        BlockingQueue<Event<QueueConsumer>> q = new LinkedBlockingQueue<>();
        WorkerConfirmListener conf = new WorkerConfirmListener(q);
        RabbitTaskInformation rabbitTaskInfo_100 = new RabbitTaskInformation("100");
        RabbitTaskInformation rabbitTaskInfo_200 = new RabbitTaskInformation("200");
        rabbitTaskInfo_100.incrementResponseCount(true);
        conf.registerResponseSequence(1,rabbitTaskInfo_100);
        conf.clearConfirmations();
        rabbitTaskInfo_200.incrementResponseCount(true);
        conf.registerResponseSequence(2, rabbitTaskInfo_200);
        conf.handleAck(4, true);
        Event<QueueConsumer> e = q.poll(1000, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(e);
        Assert.assertTrue(e instanceof ConsumerAckEvent);
        Assert.assertEquals(200L, ((ConsumerAckEvent) e).getTag());
        Event<QueueConsumer> e3 = q.poll(1000, TimeUnit.MILLISECONDS);
        Assert.assertNull(e3);
    }

    @Test(expectedExceptions = IllegalStateException.class)
    public void testDuplicateRegister()
    {
        BlockingQueue<Event<QueueConsumer>> q = new LinkedBlockingQueue<>();
        WorkerConfirmListener conf = new WorkerConfirmListener(q);
        conf.registerResponseSequence(1, new RabbitTaskInformation("100"));
        conf.registerResponseSequence(1, new RabbitTaskInformation("100"));
    }
    
    @Test
    public void testAckSingleTaskMultiplePublish()
        throws IOException, InterruptedException
    {
        BlockingQueue<Event<QueueConsumer>> q = new LinkedBlockingQueue<>();
        WorkerConfirmListener conf = new WorkerConfirmListener(q);
        RabbitTaskInformation rabbitTaskInfo_100 = new RabbitTaskInformation("100");
        rabbitTaskInfo_100.incrementResponseCount(false);
        conf.registerResponseSequence(5, rabbitTaskInfo_100);
        rabbitTaskInfo_100.incrementResponseCount(false);
        conf.registerResponseSequence(1,rabbitTaskInfo_100);
        rabbitTaskInfo_100.incrementResponseCount(true);
        conf.registerResponseSequence(2, rabbitTaskInfo_100);
        conf.handleAck(5, true);
        Event<QueueConsumer> e = q.poll(1000, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(e);
        Assert.assertTrue(e instanceof ConsumerAckEvent);
        Assert.assertEquals(100L, ((ConsumerAckEvent) e).getTag());
    }
    
    @Test
    public void testNackSingleTaskMultiplePublish()
        throws IOException, InterruptedException
    {
        BlockingQueue<Event<QueueConsumer>> q = new LinkedBlockingQueue<>();
        WorkerConfirmListener conf = new WorkerConfirmListener(q);
        RabbitTaskInformation rabbitTaskInfo_100 = new RabbitTaskInformation("100");
        rabbitTaskInfo_100.incrementResponseCount(false);
        conf.registerResponseSequence(5, rabbitTaskInfo_100);
        rabbitTaskInfo_100.incrementResponseCount(false);
        conf.registerResponseSequence(1,rabbitTaskInfo_100);
        rabbitTaskInfo_100.incrementResponseCount(true);
        conf.registerResponseSequence(2, rabbitTaskInfo_100);
        conf.handleNack(4, true);
        Event<QueueConsumer> e = q.poll(1000, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(e);
        Assert.assertTrue(e instanceof ConsumerRejectEvent);
        Assert.assertEquals(100L, ((ConsumerRejectEvent) e).getTag());
        Event<QueueConsumer> e3 = q.poll(1000, TimeUnit.MILLISECONDS);
        Assert.assertNull(e3);
        conf.handleAck(5, true);
        Event<QueueConsumer> e4 = q.poll(1000, TimeUnit.MILLISECONDS);
        Assert.assertNull(e4);
    }
    
    @Test(expectedExceptions = IllegalStateException.class)
    public void testAckSingleTaskMultiplePublishDuplicate()
        throws IOException, InterruptedException
    {
        BlockingQueue<Event<QueueConsumer>> q = new LinkedBlockingQueue<>();
        WorkerConfirmListener conf = new WorkerConfirmListener(q);
        RabbitTaskInformation rabbitTaskInfo_100 = new RabbitTaskInformation("100");
        rabbitTaskInfo_100.incrementResponseCount(false);
        conf.registerResponseSequence(5, rabbitTaskInfo_100);
        rabbitTaskInfo_100.incrementResponseCount(false);
        conf.registerResponseSequence(1,rabbitTaskInfo_100);
        rabbitTaskInfo_100.incrementResponseCount(true);
        conf.registerResponseSequence(2, rabbitTaskInfo_100);
        conf.handleAck(5, true);
        Event<QueueConsumer> e = q.poll(1000, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(e);
        Assert.assertTrue(e instanceof ConsumerAckEvent);
        Assert.assertEquals(100L, ((ConsumerAckEvent) e).getTag());
        conf.handleAck(2, false);
    }
    
    @Test
    public void testNackSingleTaskMultiplePublishDuplicate()
        throws IOException, InterruptedException
    {
        BlockingQueue<Event<QueueConsumer>> q = new LinkedBlockingQueue<>();
        WorkerConfirmListener conf = new WorkerConfirmListener(q);
        RabbitTaskInformation rabbitTaskInfo_100 = new RabbitTaskInformation("100");
        rabbitTaskInfo_100.incrementResponseCount(false);
        conf.registerResponseSequence(5, rabbitTaskInfo_100);
        rabbitTaskInfo_100.incrementResponseCount(false);
        conf.registerResponseSequence(1,rabbitTaskInfo_100);
        rabbitTaskInfo_100.incrementResponseCount(true);
        conf.registerResponseSequence(2, rabbitTaskInfo_100);
        conf.handleNack(4, true);
        Event<QueueConsumer> e = q.poll(1000, TimeUnit.MILLISECONDS);
        Assert.assertNotNull(e);
        Assert.assertTrue(e instanceof ConsumerRejectEvent);
        Assert.assertEquals(100L, ((ConsumerRejectEvent) e).getTag());
        conf.handleNack(5, false);
        Event<QueueConsumer> e5 = q.poll(1000, TimeUnit.MILLISECONDS);
        Assert.assertNull(e5);
    }
}
