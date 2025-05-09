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
import com.github.workerframework.api.TrackingInfo;
import com.github.workerframework.testworker.TestWorkerTask;
import com.github.workerframework.api.TaskMessage;
import com.github.workerframework.api.TaskStatus;
import com.github.workerframework.util.rabbitmq.QueueCreator;
import com.github.workerframework.util.rabbitmq.RabbitHeaders;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.AMQP;
import org.testng.annotations.Test;
import org.testng.Assert;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;

public class GetWorkerNameIT extends TestWorkerTestBase {
    private static final String POISON_ERROR_MESSAGE = "could not process the item.";
    private static final String WORKER_FRIENDLY_NAME = "TestWorker";
    private static final String TEST_WORKER_NAME = "testWorkerIdentifier";
    private static final String WORKER_IN = "worker-in";
    private static final String TESTWORKER_OUT = "testworker-out";
    private static final int TASK_NUMBER = 1;
    private static final Codec codec = new JsonCodec();

    @Test
    public void getWorkerNameInPoisonMessageTest() throws Exception {

        try(final Connection connection = connectionFactory.newConnection()) {

            final Channel channel = connection.createChannel();

            final Map<String, Object> args = new HashMap<>();
            args.put(QueueCreator.RABBIT_PROP_QUEUE_TYPE, QueueCreator.RABBIT_PROP_QUEUE_TYPE_QUORUM);

            channel.queueDeclare(TESTWORKER_OUT, true, false, false, args);

            final TestWorkerQueueConsumer poisonConsumer = new TestWorkerQueueConsumer();
            channel.basicConsume(TESTWORKER_OUT, true, poisonConsumer);

            final Map<String, Object> retryLimitHeaders = new HashMap<>();
            retryLimitHeaders.put(RabbitHeaders.RABBIT_HEADER_CAF_DELIVERY_COUNT, 2);

            final AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .headers(retryLimitHeaders)
                    .contentType("application/json")
                    .deliveryMode(2)
                    .build();

            final TaskMessage requestTaskMessage = new TaskMessage();

            final TestWorkerTask documentWorkerTask = new TestWorkerTask();
            documentWorkerTask.setPoison(false);
            requestTaskMessage.setTaskId(Integer.toString(TASK_NUMBER));
            requestTaskMessage.setTaskClassifier(TEST_WORKER_NAME);
            requestTaskMessage.setTaskApiVersion(TASK_NUMBER);
            requestTaskMessage.setTaskStatus(TaskStatus.NEW_TASK);
            requestTaskMessage.setTaskData(codec.serialise(documentWorkerTask));
            requestTaskMessage.setTo(WORKER_IN);

            //  Needed for minimization update to create the partial ref for the datastore.
            final var trackingInfo = new TrackingInfo("taskName" + TASK_NUMBER, new Date(), 1, null, "pipe", WORKER_IN);
            requestTaskMessage.setTracking(trackingInfo);

            channel.basicPublish("", WORKER_IN, properties, codec.serialise(requestTaskMessage));

            try {
                for (int i=0; i<100; i++){

                    Thread.sleep(100);

                    if (poisonConsumer.getLastDeliveredBody() != null){
                        break;
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            
            // With the minimization update we expect to get the message in the datastore
            final String poisonMessageStorageRef = getTaskMessageStorageRef(poisonConsumer);
            final var poisonMessageByteArrayOpt = readFileFromWebDAV(poisonMessageStorageRef);
            Assert.assertTrue(poisonMessageByteArrayOpt.isPresent(), "Minimized message should have been found");
            
            final TaskMessage decodedBody = codec.deserialise(poisonMessageByteArrayOpt.get(), TaskMessage.class);
            final String taskData = new String(decodedBody.getTaskData(), StandardCharsets.UTF_8);

            Assert.assertTrue(taskData.contains(POISON_ERROR_MESSAGE));
            Assert.assertTrue(taskData.contains(WORKER_FRIENDLY_NAME));
        }
    }
}
