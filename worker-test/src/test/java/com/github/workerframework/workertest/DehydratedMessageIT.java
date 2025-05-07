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
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import static com.github.workerframework.util.rabbitmq.RabbitHeaders.RABBIT_HEADER_CAF_DEHYDRATION_ID;

public class DehydratedMessageIT extends TestWorkerTestBase{
    private static final String TEST_WORKER_NAME = "testWorkerIdentifier";
    private static final String WORKER_IN = "worker-in";
    private static final String TESTWORKER_OUT = "testworker-out";
    private static final Codec codec = new JsonCodec();

    @Test
    public void checkDehydratedMessageIsRecovered() throws CodecException, IOException, TimeoutException 
    {
        try(final Connection connection = connectionFactory.newConnection()) {
            final Channel channel = prepareChannel(connection);
            
            // This publish will result in a dehydratedMessage created by the publisher
            final int taskNumber1 = 1;
            final TestWorkerQueueConsumer setupMessageConsumer = new TestWorkerQueueConsumer();
            publish(channel, taskNumber1, new HashMap<>());
            consume(channel, setupMessageConsumer);
            channel.close();
            final String taskMessageStorageRef = getTaskMessageStorageRef(setupMessageConsumer);
            final String webdav_url = System.getProperty("webdav_url");
            final String webdavPath = String.format("%s/%s", webdav_url, taskMessageStorageRef);

            Assert.assertTrue(dehydratedMessageExists(webdavPath), "Dehydrated message not found at " + webdavPath);

            //  Now we can send a message which expects to find the publishers deyhdrated message.
            final Channel channel2 = prepareChannel(connection);
            final Map<String, Object> headers = new HashMap<>();
            headers.put(RABBIT_HEADER_CAF_DEHYDRATION_ID, taskMessageStorageRef);
            final int taskNumber2 = 2;
            publish(channel2, taskNumber2, headers);
            final TestWorkerQueueConsumer testMessageConsumer = new TestWorkerQueueConsumer();
            consume(channel2, testMessageConsumer);
            final String testTaskMessageStorageRef = getTaskMessageStorageRef(testMessageConsumer);
            Assert.assertNotEquals(testTaskMessageStorageRef, taskMessageStorageRef, "Storage refs should have been different");

            final TaskMessage taskMessage = codec.deserialise(testMessageConsumer.getLastDeliveredBody(), TaskMessage.class);
            Assert.assertEquals(taskMessage.getTaskClassifier(), "TestWorkerResult", "Task classifier is wrong");

            final var publishedHeaders = testMessageConsumer.getHeaders();
            Assert.assertTrue(publishedHeaders.containsKey(RABBIT_HEADER_CAF_DEHYDRATION_ID), "Should have the dehydration header:" + publishedHeaders);
            
            // The previously dehydrated message should now have been deleted by the confirm listener
            Assert.assertFalse(dehydratedMessageExists(webdavPath), "Dehydrated message should not have been found");
        }
    }
    
    public void consume(
        final Channel channel,
        final TestWorkerQueueConsumer messageConsumer
    ) throws IOException, TimeoutException {
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

    public String getTaskMessageStorageRef(
        final TestWorkerQueueConsumer messageConsumer
    )
    {
        final Map<String, Object> outgoingHeaders = messageConsumer.getHeaders();
        final Optional<String> outgoingTaskMessageStorageRef = outgoingHeaders.containsKey(RABBIT_HEADER_CAF_DEHYDRATION_ID) ?
            Optional.of(outgoingHeaders.get(RABBIT_HEADER_CAF_DEHYDRATION_ID).toString()) :
            Optional.empty();
        Assert.assertTrue(outgoingTaskMessageStorageRef.isPresent(), "The dehydration header was missing");
        return outgoingTaskMessageStorageRef.get();
    }
    
    private void publish(
        final Channel channel, 
        final int taskNumber, 
        final Map<String, Object> headers
    ) throws CodecException, IOException {
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

        final AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
            .contentType("application/json")
            .deliveryMode(2)
            .headers(headers)
            .build();

        channel.basicPublish("", WORKER_IN, properties, codec.serialise(requestTaskMessage)); 
    }

    public static boolean dehydratedMessageExists(final String path) throws IOException 
    {
        
        URL url = new URL(path);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        return connection.getResponseCode() == HttpURLConnection.HTTP_OK;
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
