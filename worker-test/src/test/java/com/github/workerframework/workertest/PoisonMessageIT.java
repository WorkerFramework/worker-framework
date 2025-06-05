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

    private static final String POISON_MESSAGE_IT_OFFLOADING_IN = "PoisonMessageIT-Offloading-in";
    private static final String POISON_MESSAGE_IT_OFFLOADING_OUT = "PoisonMessageIT-Offloading-out";
    private static final String POISON_MESSAGE_IT_OFFLOADING_INVALID = "PoisonMessageIT-Offloading-invalid";

    private static final int TASK_NUMBER = 1;
    private static final Codec codec = new JsonCodec();

    @Test
    public void getWorkerNameInPoisonMessageTest() throws Exception {

        try(final Connection connection = connectionFactory.newConnection();
            final Channel channel = connection.createChannel()) {

            final Map<String, Object> args = new HashMap<>();
            args.put(QueueCreator.RABBIT_PROP_QUEUE_TYPE, QueueCreator.RABBIT_PROP_QUEUE_TYPE_QUORUM);
            channel.queueDeclare(POISON_MESSAGE_IT_IN, true, false, false, args);

            final TaskMessage requestTaskMessage = new TaskMessage();

            final TestWorkerTask documentWorkerTask = new TestWorkerTask();
            documentWorkerTask.setPoison(true);
            requestTaskMessage.setTaskId(Integer.toString(TASK_NUMBER));
            requestTaskMessage.setTaskClassifier(TEST_WORKER_NAME);
            requestTaskMessage.setTaskApiVersion(TASK_NUMBER);
            requestTaskMessage.setTaskStatus(TaskStatus.NEW_TASK);
            requestTaskMessage.setTaskData(codec.serialise(documentWorkerTask));
            requestTaskMessage.setTo(POISON_MESSAGE_IT_IN);

            final AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .deliveryMode(2)
                    .build();

            channel.basicPublish("", POISON_MESSAGE_IT_IN, properties, codec.serialise(requestTaskMessage));

            final TestWorkerQueueConsumer poisonConsumer = new TestWorkerQueueConsumer();
            channel.queueDeclare(POISON_MESSAGE_IT_OUT, true, false, false, args);

            channel.basicConsume(POISON_MESSAGE_IT_OUT, true, poisonConsumer);

            try {
                for (int i=0; i<10000; i++){

                    Thread.sleep(100);

                    if (poisonConsumer.getLastDeliveredBody() != null){
                        break;
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            Assert.assertNotNull(poisonConsumer.getLastDeliveredBody());
            final TaskMessage decodedBody = codec.deserialise(poisonConsumer.getLastDeliveredBody(), TaskMessage.class);
            
            final String taskData = new String(decodedBody.getTaskData(), StandardCharsets.UTF_8);

            Assert.assertTrue(taskData.contains(WORKER_FRIENDLY_NAME));
            Assert.assertTrue(taskData.contains(POISON_ERROR_MESSAGE));
        }
    }

    @Test
    public void offloadedPoisonMessageGoesToRejectFolderTest() throws Exception {
        try(final Connection connection = connectionFactory.newConnection();
            final Channel channel = prepareChannel(connection, POISON_MESSAGE_IT_OFFLOADING_IN, 
                    POISON_MESSAGE_IT_OFFLOADING_OUT, POISON_MESSAGE_IT_OFFLOADING_INVALID)) {
            final TestWorkerTask documentWorkerTask = new TestWorkerTask();
            documentWorkerTask.setPoison(true);
            
            final var taskMessage = getTaskMessage(
                TEST_WORKER_NAME,
                TASK_NUMBER,
                documentWorkerTask,
                POISON_MESSAGE_IT_OFFLOADING_IN
            );

            final byte[] taskData = taskMessage.getTaskData();
            taskMessage.setTaskData(null);
            final var storageRef = UUID.randomUUID().toString();
            writeFileToWebDav(storageRef, taskData);
            final HashMap<String, Object> publishHeaders = new HashMap<>();
            publishHeaders.put(RABBIT_HEADER_CAF_PAYLOAD_OFFLOADING_STORAGE_REF, storageRef);
            
            // Publish a message to the test worker, the worker should detect this as a poison message
            // because the payload is already offloaded it should remain offloaded and the message should be sent to the reject queue.
            publish(
                channel,
                codec.serialise(taskMessage),
                publishHeaders,
                POISON_MESSAGE_IT_OFFLOADING_IN
            );

            //  Now we can consume the outgoing message from the reject queue.
            final TestWorkerQueueConsumer consumer = new TestWorkerQueueConsumer();
            consume(channel, consumer, POISON_MESSAGE_IT_OFFLOADING_INVALID);
            final var rejectedTaskMessageStorageRef = getTaskMessageStorageRef(consumer);
            Assert.assertTrue(rejectedTaskMessageStorageRef.isPresent(), "The payload offloading header was missing");
            // The rejected message should be present in the datastore
            final var rejectedByteArrayOpt = readFileFromWebDAV(rejectedTaskMessageStorageRef.get());
            Assert.assertTrue(rejectedByteArrayOpt.isPresent(), "Offloaded payload should have been found");
        }
    }
}
