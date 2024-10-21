/*
 * Copyright 2015-2024 Open Text.
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
package com.hpe.caf.worker.workertest;

import com.github.cafapi.common.api.Codec;
import com.github.cafapi.common.api.CodecException;
import com.github.cafapi.common.codecs.jsonlzf.JsonCodec;
import com.github.workerframework.testworker.TestWorkerTask;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.util.rabbitmq.QueueCreator;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.AMQP;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class PoisonMessageIT  extends TestWorkerTestBase{
    private static final String POISON_ERROR_MESSAGE = "could not process the item.";
    private static final String WORKER_FRIENDLY_NAME = "TestWorker";
    private static final String TEST_WORKER_NAME = "testWorkerIdentifier";
    private static final String WORKER_IN = "worker-in";
    private static final String TESTWORKER_OUT = "testworker-out";
    private static final int TASK_NUMBER = 1;
    private static final Codec codec = new JsonCodec();

    @Test
    public void getWorkerNameInPoisonMessageTest() throws IOException, TimeoutException, CodecException {

        try(final Connection connection = connectionFactory.newConnection()) {

            final Channel channel = connection.createChannel();

            final Map<String, Object> args = new HashMap<>();
            args.put(QueueCreator.RABBIT_PROP_QUEUE_TYPE, QueueCreator.RABBIT_PROP_QUEUE_TYPE_QUORUM);
            channel.queueDeclare(WORKER_IN, true, false, false, args);

            final TaskMessage requestTaskMessage = new TaskMessage();

            final TestWorkerTask documentWorkerTask = new TestWorkerTask();
            documentWorkerTask.setPoison(true);
            requestTaskMessage.setTaskId(Integer.toString(TASK_NUMBER));
            requestTaskMessage.setTaskClassifier(TEST_WORKER_NAME);
            requestTaskMessage.setTaskApiVersion(TASK_NUMBER);
            requestTaskMessage.setTaskStatus(TaskStatus.NEW_TASK);
            requestTaskMessage.setTaskData(codec.serialise(documentWorkerTask));
            requestTaskMessage.setTo(WORKER_IN);

            final AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .contentType("application/json")
                    .deliveryMode(2)
                    .build();

            channel.basicPublish("", WORKER_IN, properties, codec.serialise(requestTaskMessage));

            final TestWorkerQueueConsumer poisonConsumer = new TestWorkerQueueConsumer();
            channel.queueDeclare(TESTWORKER_OUT, true, false, false, args);

            channel.basicConsume(TESTWORKER_OUT, false, poisonConsumer);

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

            Assert.assertTrue(taskData.contains(POISON_ERROR_MESSAGE));
            Assert.assertTrue(taskData.contains(WORKER_FRIENDLY_NAME));
        }
    }
}
