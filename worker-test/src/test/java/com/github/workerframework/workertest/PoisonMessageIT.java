/*
 * Copyright 2015-2026 Open Text.
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
import com.github.cafapi.common.codecs.json.JsonCodec;
import com.github.workerframework.testworker.TestWorkerTask;
import com.github.workerframework.api.TaskMessage;
import com.github.workerframework.api.TaskStatus;
import com.github.workerframework.util.rabbitmq.QueueCreator;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.AMQP;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.github.workerframework.util.rabbitmq.RabbitHeaders.RABBIT_HEADER_CAF_PAYLOAD_OFFLOADING_STORAGE_REF;

public class PoisonMessageIT  extends WorkerTestBase {
    private static final String POISON_ERROR_MESSAGE = "could not process the item.";
    private static final String WORKER_FRIENDLY_NAME = "TestWorker";
    private static final String TEST_WORKER_NAME = "PoisonMessageIT";
    private static final String POISON_MESSAGE_IT_IN = "PoisonMessageIT-in";
    private static final String POISON_MESSAGE_IT_OUT = "PoisonMessageIT-out";
    private static final String POISON_MESSAGE_IT_REJECT = "PoisonMessageIT-reject";

    private static final String POISON_MESSAGE_IT_OFFLOADING_IN = "PoisonMessageIT-Offloading-in";
    private static final String POISON_MESSAGE_IT_OFFLOADING_OUT = "PoisonMessageIT-Offloading-out";
    private static final String POISON_MESSAGE_IT_OFFLOADING_REJECT = "PoisonMessageIT-Offloading-reject";

    private static final int TASK_NUMBER = 1;
    private static final Codec codec = new JsonCodec();

    @Test
    public void getWorkerNameInPoisonMessageTest() throws Exception {

        try(final Connection connection = connectionFactory.newConnection();
            final Channel channel = connection.createChannel()) {
            createQueues(channel, POISON_MESSAGE_IT_IN, POISON_MESSAGE_IT_OUT, POISON_MESSAGE_IT_REJECT);

            final TaskMessage requestTaskMessage = new TaskMessage();

            final TestWorkerTask testWorkerTask = new TestWorkerTask();
            testWorkerTask.setPoison(true);
            requestTaskMessage.setTaskId(Integer.toString(TASK_NUMBER));
            requestTaskMessage.setTaskClassifier(TEST_WORKER_NAME);
            requestTaskMessage.setTaskApiVersion(TASK_NUMBER);
            requestTaskMessage.setTaskStatus(TaskStatus.NEW_TASK);
            requestTaskMessage.setTaskData(codec.serialise(testWorkerTask));
            requestTaskMessage.setTo(POISON_MESSAGE_IT_IN);

            final AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .deliveryMode(2)
                    .build();

            channel.basicPublish("", POISON_MESSAGE_IT_IN, properties, codec.serialise(requestTaskMessage));

            final TestWorkerQueueConsumer rejectConsumer = new TestWorkerQueueConsumer();
            
            //Verify a copy was placed on the reject queue for later inspection
            consume(channel, rejectConsumer, POISON_MESSAGE_IT_REJECT);

            Assert.assertNotNull(rejectConsumer.getLastDeliveredBody(), 
                    "Message was not delivered to the queue before timeout or not at all.");
            
            final TaskMessage rejectTaskMessage = codec.deserialise(rejectConsumer.getLastDeliveredBody(), TaskMessage.class);
            
            Assert.assertEquals(rejectTaskMessage.getTaskStatus(), TaskStatus.RESULT_EXCEPTION);
            
            final TestWorkerTask copyOfTestWorkerTask = codec.deserialise(rejectTaskMessage.getTaskData(), TestWorkerTask.class);
            
            Assert.assertEquals(copyOfTestWorkerTask.isPoison(), testWorkerTask.isPoison());

            final TestWorkerQueueConsumer outConsumer = new TestWorkerQueueConsumer();
            consume(channel, outConsumer, POISON_MESSAGE_IT_OUT);
            //Verify a response was placed on the out queue for further processing

            Assert.assertNotNull(outConsumer.getLastDeliveredBody(),
                    "Message was not delivered to the queue before timeout or not at all.");
            
            final TaskMessage outputTaskMessage = codec.deserialise(outConsumer.getLastDeliveredBody(), TaskMessage.class);
            final String outputTaskData = new String(outputTaskMessage.getTaskData(), StandardCharsets.UTF_8);

            Assert.assertTrue(outputTaskData.contains(WORKER_FRIENDLY_NAME));
            Assert.assertTrue(outputTaskData.contains(POISON_ERROR_MESSAGE));

            Assert.assertEquals(outputTaskMessage.getTaskStatus(), TaskStatus.RESULT_SUCCESS);
            
        }
    }

    @Test
    public void offloadedPoisonMessageGoesToRejectFolderTest() throws Exception {
        try(final Connection connection = connectionFactory.newConnection();
            final Channel channel = prepareChannel(connection)) {
            createQueues(channel, 
                    POISON_MESSAGE_IT_OFFLOADING_IN, POISON_MESSAGE_IT_OFFLOADING_OUT, POISON_MESSAGE_IT_OFFLOADING_REJECT);
            
            final TestWorkerTask testWorkerTask = new TestWorkerTask();
            testWorkerTask.setPoison(true);
            
            final var taskMessage = getTaskMessage(
                TEST_WORKER_NAME,
                TASK_NUMBER,
                testWorkerTask,
                POISON_MESSAGE_IT_OFFLOADING_IN
            );

            final byte[] taskData = taskMessage.getTaskData();
            taskMessage.setTaskData(null);
            final var storageRef = UUID.randomUUID().toString();
            writeFileToWebDav(storageRef, taskData);
            final HashMap<String, Object> publishHeaders = new HashMap<>();
            publishHeaders.put(RABBIT_HEADER_CAF_PAYLOAD_OFFLOADING_STORAGE_REF, storageRef);
            
            // Publish a message to the test worker, the worker should detect this as a poison message
            // because the payload is already offloaded it should remain offloaded and a copy should be placed on 
            // the reject queue.
            publish(
                channel,
                codec.serialise(taskMessage),
                publishHeaders,
                POISON_MESSAGE_IT_OFFLOADING_IN
            );

            //  Now we can consume the outgoing message from the reject queue.
            final TestWorkerQueueConsumer rejectConsumer = new TestWorkerQueueConsumer();
            consume(channel, rejectConsumer, POISON_MESSAGE_IT_OFFLOADING_REJECT);

            final var rejectedTaskMessageStorageRef = getTaskMessageStorageRef(rejectConsumer);

            Assert.assertTrue(rejectedTaskMessageStorageRef.isPresent(), "The payload offloading header was missing");

            // The rejected message should be present in the datastore
            final var rejectedByteArrayOpt = readFileFromWebDAV(rejectedTaskMessageStorageRef.get());
            Assert.assertTrue(rejectedByteArrayOpt.isPresent(), "Offloaded payload should have been found");

            final TestWorkerTask rejectTestWorkerTask = codec.deserialise(rejectedByteArrayOpt.get(), 
                    TestWorkerTask.class);
            
            Assert.assertEquals(rejectTestWorkerTask.isPoison(), testWorkerTask.isPoison());

            final TestWorkerQueueConsumer outConsumer = new TestWorkerQueueConsumer();
            consume(channel, outConsumer, POISON_MESSAGE_IT_OFFLOADING_OUT);
            //Verify a response was placed on the out queue for further processing

            Assert.assertNotNull(outConsumer.getLastDeliveredBody(),
                    "Message was not delivered to the queue before timeout or not at all.");

            final TaskMessage outputTaskMessage = codec.deserialise(outConsumer.getLastDeliveredBody(), TaskMessage.class);
            outputTaskMessage.setTaskStatus(TaskStatus.RESULT_SUCCESS);

            final var outputTaskMessageStorageRef = getTaskMessageStorageRef(outConsumer);

            Assert.assertTrue(outputTaskMessageStorageRef.isPresent(), "Offloaded payload should have been found");

            Assert.assertNotEquals(outputTaskMessageStorageRef.get(), rejectedTaskMessageStorageRef.get(),
                    "The output reference should not match the rejected reference");
            
            final var outputOffloadedPayloadBytes = readFileFromWebDAV(outputTaskMessageStorageRef.get());
            Assert.assertTrue(outputOffloadedPayloadBytes.isPresent(), "Offloaded payload bytes should have been found");
            
            final var outputTaskData = new String(outputOffloadedPayloadBytes.get(), StandardCharsets.UTF_8);

            Assert.assertTrue(outputTaskData.contains(WORKER_FRIENDLY_NAME));
            Assert.assertTrue(outputTaskData.contains(POISON_ERROR_MESSAGE));

            Assert.assertEquals(outputTaskMessage.getTaskStatus(), TaskStatus.RESULT_SUCCESS);
            
        }
    }
}
