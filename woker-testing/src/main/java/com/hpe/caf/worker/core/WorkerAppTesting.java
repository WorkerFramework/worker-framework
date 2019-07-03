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
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.hpe.caf.worker.core;


import com.codahale.metrics.health.HealthCheckRegistry;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.MessagePriorityManager;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskRejectedException;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.TrackingInfo;
import com.hpe.caf.api.worker.Worker;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.api.worker.WorkerFactory;
import com.hpe.caf.api.worker.WorkerResponse;
import com.hpe.caf.codec.JsonCodec;
import com.hpe.caf.configs.RabbitConfiguration;
import com.hpe.caf.naming.ServicePath;
import com.hpe.caf.util.rabbitmq.ConsumerAckEvent;
import com.hpe.caf.util.rabbitmq.ConsumerDeliverEvent;
import com.hpe.caf.util.rabbitmq.ConsumerRejectEvent;
import com.hpe.caf.util.rabbitmq.Delivery;
import com.hpe.caf.util.rabbitmq.Event;
import com.hpe.caf.util.rabbitmq.QueueConsumer;
import com.hpe.caf.util.rabbitmq.RabbitConsumer;
import com.hpe.caf.util.rabbitmq.RabbitUtil;
import com.hpe.caf.worker.queue.rabbit.RabbitWorkerQueue;
import com.hpe.caf.worker.queue.rabbit.RabbitWorkerQueueConfiguration;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConfirmListener;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.jodah.lyra.ConnectionOptions;
import net.jodah.lyra.Connections;
import net.jodah.lyra.config.Config;
import org.mockito.Mockito;

/**
 *
 * @author Gaana
 */
public class WorkerAppTesting 
{
    
    public static void main (String []args) throws Exception {
        WorkerAppTesting x = new WorkerAppTesting();
        x.run(args);
    }
   
    public void run(String[] args) throws Exception
    {
        
        BlockingQueue<byte[]> q = new LinkedBlockingQueue<>();
        Codec codec = new JsonCodec();
        WorkerThreadPool wtp = WorkerThreadPool.create(5);        
        ServicePath path = new ServicePath("/some/Path/not/sure");
        TestWorkerTask task = new TestWorkerTask();
               
        RabbitWorkerQueueConfiguration rabbitWorkerConfig = new RabbitWorkerQueueConfiguration();
        rabbitWorkerConfig.setInputQueue("gaana");
        rabbitWorkerConfig.setRetryQueue("gaana");
        rabbitWorkerConfig.setRejectedQueue("gaana-reject");
        
        rabbitWorkerConfig.setPrefetchBuffer(10);
        RabbitConfiguration rabbitConfig = new RabbitConfiguration();
        rabbitConfig.setRabbitHost("192.168.56.10");
        rabbitConfig.setRabbitPort(5672);
        rabbitConfig.setRabbitUser("guest");
        rabbitConfig.setRabbitPassword("guest");
        rabbitWorkerConfig.setRabbitConfiguration(rabbitConfig);
        RabbitWorkerQueue queue = new RabbitWorkerQueue(rabbitWorkerConfig, 1);
        MessagePriorityManager priorityManager = Mockito.mock(MessagePriorityManager.class);
        Mockito.when(priorityManager.getResponsePriority(Mockito.any())).thenReturn(2);
        HealthCheckRegistry healthCheckRegistry = Mockito.mock(HealthCheckRegistry.class);
        TransientHealthCheck transientHealthCheck = Mockito.mock(TransientHealthCheck.class);
        
        WorkerCore core = new WorkerCore(codec, wtp, queue, priorityManager, getWorkerFactory(task, codec), path, healthCheckRegistry, transientHealthCheck); 
        
        ConnectionOptions lyraOpts = RabbitUtil.createLyraConnectionOptions(rabbitConfig.getRabbitHost(), rabbitConfig.getRabbitPort(), 
                                                            rabbitConfig.getRabbitUser(), rabbitConfig.getRabbitPassword());
        Config lyraConfig = RabbitUtil.createLyraConfig(1, 30, 1);
        Connection conn =Connections.create(lyraOpts, lyraConfig);  
        Channel publishingChannel = conn.createChannel();
        //Channel consumingChannel = conn.createChannel();
        
//        TestRabbitConsumer consumer = new TestRabbitConsumer(0, q, codec);
        //RabbitUtil.declareWorkerQueue(consumingChannel, rabbitWorkerConfig.getInputQueue());
        RabbitUtil.declareWorkerQueue(publishingChannel, "gaana");
        core.start();
        TaskMessage taskMessage = new TaskMessage();
        taskMessage.setTaskData(codec.serialise(new TestWorkerTask()));
        taskMessage.setTaskId("1");
        taskMessage.setTo("gaana");
        String statusurl ="http://david-int01.aspensb.local:9410/job-service/v1/jobs/ej_davide_1073_59/isActive";
        taskMessage.setTracking(new TrackingInfo("task1", new Date(), statusurl, "dataprocessing-jobtracking-in", null));
        publishingChannel.basicPublish("", rabbitWorkerConfig.getInputQueue(), null, codec.serialise(taskMessage));
        //consumingChannel.basicQos(10);
        //consumingChannel.basicConsume(rabbitWorkerConfig.getInputQueue(), consumer);
        //consumer.getDeliverEvent(envelope, data, headers)
                       
        //core.getWorkerQueue().publish(acknowledgeId, taskMessage, targetQueue, headers);
        //core.getWorkerQueue().acknowledgeTask(messageId);
        
        // at this point, the queue should hand off the task to the app, the app should get a worker from the mocked WorkerFactory,
        // and the Worker itself is a mock wrapped in a WorkerWrapper, which should return success and the appropriate result data
//        byte[] stuff = codec.serialise(getTaskMessage(task, codec, WORKER_NAME));
//        queue.submitTask(QUEUE_MSG_ID, stuff);
    }
    
    
    WorkerAppTesting()
    {
    }

    class TestRabbitConsumer extends RabbitConsumer
    {
        public TestRabbitConsumer(int pollPeriod, BlockingQueue events, Object consumerImpl)
        {
            super(pollPeriod, events, consumerImpl);
        }
        @Override
        protected Event getDeliverEvent(Envelope envelope, byte[] data, Map headers)
        {
            System.out.println("getDeliverEvent(): " + new String(data));
            return new ConsumerDeliverEvent(new Delivery(envelope, data, headers));
        }
    }
    
    class TestConfirmListener implements ConfirmListener
    {
        private final SortedMap<Long, Long> confirmMap = Collections.synchronizedSortedMap(new TreeMap<>());
        private BlockingQueue<Event<QueueConsumer>> consumerEvents;
        TestConfirmListener(BlockingQueue<Event<QueueConsumer>> events)
    {
        this.consumerEvents = Objects.requireNonNull(events);
    }
        
        /**
         * Tell the listener to keep track of a published response and its associated input task message, to ack or reject the
         * input task message as appropriate when RabbitMQ calls back.
         *
         * @param publishSequence the published sequence ID of the Worker response message
         * @param ackId the incoming task message ID to ack when the published response is confirmed
         */
        public void registerResponseSequence(long publishSequence, long ackId)
        {
            if (confirmMap.putIfAbsent(publishSequence, ackId) != null) {
                throw new IllegalStateException("Sequence id " + publishSequence + " already present in confirmations map");
            }            
            confirmMap.put(publishSequence, ackId);
        }


        @Override
        public void handleAck(long sequenceNo, boolean multiple)
            throws IOException
        {            
            handle(sequenceNo, multiple, ConsumerAckEvent::new);
        }

        @Override
        public void handleNack(long sequenceNo, boolean multiple)
            throws IOException
        {            
            handle(sequenceNo, multiple, ConsumerRejectEvent::new);
        }

        private void handle(long sequenceNo, boolean multiple, Function<Long, Event<QueueConsumer>> eventSource)
        {
            if (multiple) {
                Map<Long, Long> ackMap = confirmMap.headMap(sequenceNo + 1);
                synchronized (confirmMap) {
                    consumerEvents.addAll(ackMap.values().stream().map(eventSource::apply).collect(Collectors.toList()));
                }
                ackMap.clear(); // clear all entries up to this (n)acked sequence number
            } else {
                Long ackId = confirmMap.remove(sequenceNo);
                if (ackId == null) {                    
                    throw new IllegalStateException("Sequence number " + sequenceNo + " not found in WorkerConfirmListener");
                } else {
                    consumerEvents.add(eventSource.apply(ackId));
                }
            }
        }
    }
    
    
    public class TestWorkerTask
    {
        private String data = "test123";

        public TestWorkerTask()
        {
        }

        public String getData()
        {
            return data;
        }

        public void setData(final String data)
        {
            this.data = data;
        }
    }
    
    private WorkerFactory getWorkerFactory(final TestWorkerTask task, final Codec codec)
        throws WorkerException
    {
        TestWorker testWorker = new TestWorker(task);
        WorkerFactory factory = Mockito.mock(WorkerFactory.class);
//        Worker mockWorker = Mockito.mock(Worker.class);
        Mockito.when(factory.getWorker(Mockito.any())).thenReturn(testWorker);
        return factory;
    }
    public class TestWorker implements Worker
    {
        
        TestWorkerTask testWorkerTask;
        
        public TestWorker(TestWorkerTask testWorkerTask){
            this.testWorkerTask = testWorkerTask;
        }
        
        @Override
        public WorkerResponse doWork() throws InterruptedException, TaskRejectedException, InvalidTaskException
        {
            System.out.println(testWorkerTask.getData());
            WorkerResponse workerResponse = new WorkerResponse("gaana-out", TaskStatus.RESULT_SUCCESS, testWorkerTask.getData().getBytes(), "gaaType", 1, null);
            return workerResponse;
            
        }

        @Override
        public String getWorkerIdentifier()
        {
            return "gaaType";
        }

        @Override
        public int getWorkerApiVersion()
        {
            return 1;
        }

        @Override
        public WorkerResponse getGeneralFailureResult(Throwable t)
        {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }
}
