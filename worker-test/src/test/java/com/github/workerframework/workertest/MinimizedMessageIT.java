/*
 * Copyright 2015-2025 Open Text.
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
package com.github.workerframework.workertest;

import com.github.cafapi.common.api.Codec;
import com.github.cafapi.common.api.CodecException;
import com.github.cafapi.common.codecs.json.JsonCodec;
import com.github.workerframework.api.TaskMessage;
import com.github.workerframework.api.TaskStatus;
import com.github.workerframework.api.TrackingInfo;
import com.github.workerframework.testworker.TestWorkerTask;
import com.github.workerframework.util.rabbitmq.QueueCreator;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static com.github.workerframework.util.rabbitmq.RabbitHeaders.RABBIT_HEADER_CAF_MINIMIZATION_ID;

public class MinimizedMessageIT extends TestWorkerTestBase {
    private static final String TEST_WORKER_NAME = "testWorkerIdentifier";
    private static final String WORKER_IN = "worker-in";
    private static final String TESTWORKER_OUT = "testworker-out";
    private static final Codec codec = new JsonCodec();    

    @Test
    public void checkMinimizedMessageIsConsumedAndDeletedOnAck() throws Exception {
        final String setupMinimizedMessageStorageRef = setupMinimizedMessage(1);        
        try(final Connection connection = connectionFactory.newConnection()) {
            final Channel channel = prepareChannel(connection);
            //  Now we can send a message which expects to find the taskMessageStorageRef.
            final Map<String, Object> headers = new HashMap<>();
            headers.put(RABBIT_HEADER_CAF_MINIMIZATION_ID, setupMinimizedMessageStorageRef);
            // this publish will result in a minimizedMessage being recovered by the consumer.
            // the body will be ignored as the minimized message will be used.
            publish(channel, new byte[0], headers);
            
            final TestWorkerQueueConsumer consumer = new TestWorkerQueueConsumer();
            consume(channel, consumer);
            final String consumedTaskMessageStorageRef = getTaskMessageStorageRef(consumer);
            Assert.assertNotEquals(consumedTaskMessageStorageRef, setupMinimizedMessageStorageRef, "Storage refs should have been different");

            final var publishedHeaders = consumer.getHeaders();
            Assert.assertTrue(publishedHeaders.containsKey(RABBIT_HEADER_CAF_MINIMIZATION_ID), "Should have the minimization header:" + publishedHeaders);
            
            // The previously dehydrated message should now have been deleted by the confirm listener
            final var storedSetupByteArrayOpt = readFileFromWebDAV(setupMinimizedMessageStorageRef);
            Assert.assertFalse(storedSetupByteArrayOpt.isEmpty(), "setup message should not have been found");

            // The previously published message should be present in the datastore
            final var consumedByteArrayOpt = readFileFromWebDAV(consumedTaskMessageStorageRef);
            Assert.assertTrue(consumedByteArrayOpt.isPresent(), "Minimized message should have been found");
        }
    }
    
    public void consume(
        final Channel channel,
        final TestWorkerQueueConsumer messageConsumer
    ) throws IOException {
        channel.basicConsume(TESTWORKER_OUT, false, messageConsumer);
        try {
            for (int i = 0; i < 1000; i++) {

                Thread.sleep(100);

                if (messageConsumer.getLastDeliveredBody() != null) {
                    break;
                }
            }
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void publish(
        final Channel channel,
        final byte[] taskMessage, 
        final Map<String, Object> headers
    ) throws IOException {
        final AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
            .contentType("application/json")
            .deliveryMode(2)
            .headers(headers)
            .build();

        channel.basicPublish("", WORKER_IN, properties, taskMessage); 
    }

    /**
     * This method will send a message to the worker-in queue and return the storage ref of the minimized message stored 
     * in the datastore on publish to the worker-out queue.
     * @param taskNumber
     * @return
     * @throws IOException
     * @throws TimeoutException
     * @throws CodecException
     */
    private String setupMinimizedMessage(final int taskNumber) throws Exception {
        try(final Connection connection = connectionFactory.newConnection()) {
            final Channel channel = prepareChannel(connection);
            publish(channel, buildTaskMessageByteArray(taskNumber), new HashMap<>());
            
            final TestWorkerQueueConsumer consumer = new TestWorkerQueueConsumer();
            consume(channel, consumer);
            
            channel.close();
            
            final String taskMessageStorageRef = getTaskMessageStorageRef(consumer);
            final var storedByteArrayOpt = readFileFromWebDAV(taskMessageStorageRef);
            Assert.assertTrue(storedByteArrayOpt.isPresent(), "Minimized message not found at " + taskMessageStorageRef);
            return taskMessageStorageRef;
        }
    }
  
    private static byte[] buildTaskMessageByteArray(final int taskNumber) throws CodecException {
        final var trackingInfo = new TrackingInfo("taskName" + taskNumber, new Date(), 1, "http://hello.com", "pipe", "to");
        final TestWorkerTask documentWorkerTask = new TestWorkerTask();
        final TaskMessage requestTaskMessage = new TaskMessage();
        requestTaskMessage.setTaskId(Integer.toString(taskNumber));
        requestTaskMessage.setTaskClassifier(TEST_WORKER_NAME);
        requestTaskMessage.setTaskApiVersion(1);
        requestTaskMessage.setTaskStatus(TaskStatus.NEW_TASK);
        requestTaskMessage.setTaskData(codec.serialise(documentWorkerTask));
        requestTaskMessage.setTo(WORKER_IN);
        requestTaskMessage.setTracking(trackingInfo);
        return codec.serialise(requestTaskMessage);
    }
   
    private Channel prepareChannel(final Connection connection) throws IOException {
        final Channel channel = connection.createChannel();
        final Map<String, Object> args = new HashMap<>();
        args.put(QueueCreator.RABBIT_PROP_QUEUE_TYPE, QueueCreator.RABBIT_PROP_QUEUE_TYPE_QUORUM);
        channel.queueDeclare(WORKER_IN, true, false, false, args);
        channel.queueDeclare(TESTWORKER_OUT, true, false, false, args); 
        return channel;
    }
}
