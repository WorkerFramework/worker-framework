/*
 * Copyright 2015-2023 Open Text.
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

import com.google.common.base.Strings;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.codec.JsonCodec;
import com.hpe.caf.util.rabbitmq.QueueCreator;
import com.hpe.caf.worker.document.DocumentWorkerTask;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.Envelope;
import org.testng.annotations.Test;
import org.testng.Assert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

public class GetWorkerNameIT extends TestWorkerTestBase {
    private static final String POISON_ERROR_MESSAGE = "could not process the item.";
    private static final String WORKER_FRIENDLY_NAME = "TestWorker";
    private static final String RABBIT_RETRY_LIMIT_HEADER = "x-caf-worker-retry-limit";
    private static final String RABBIT_RETRY_COUNT_HEADER = "x-caf-worker-retry";
    private static final String RABBIT_PROP_QUEUE_TYPE_NAME = !Strings.isNullOrEmpty(System.getenv("RABBIT_PROP_QUEUE_TYPE_NAME"))?
            System.getenv("RABBIT_PROP_QUEUE_TYPE_NAME") : QueueCreator.RABBIT_PROP_QUEUE_TYPE_QUORUM;
    private static final String TEST_WORKER_NAME = "testWorkerIdentifier";
    private static final String WORKER_IN = "worker-in";
    private static final String TESTWORKER_OUT = "testworker-out";
    private static final String TASK_DATA_MESSAGE = "worker name";
    private static final String TEST_DATA = "TEST_DATA";
    private static final String TASK_DATA = "taskData";
    private static final int TASK_NUMBER = 1;
    private static final Codec codec = new JsonCodec();

    @Test
    public void getWorkerNameInPoisonMessageTest() throws IOException, TimeoutException, CodecException {

        try(final Connection connection = connectionFactory.newConnection()) {

            final Channel channel = connection.createChannel();

            final Map<String, Object> args = new HashMap<>();
            args.put(QueueCreator.RABBIT_PROP_QUEUE_TYPE, RABBIT_PROP_QUEUE_TYPE_NAME);

            channel.queueDeclare(TESTWORKER_OUT, true, false, false, args);

            final TestWorkerQueueConsumer poisonConsumer = new TestWorkerQueueConsumer();
            channel.basicConsume(TESTWORKER_OUT, true, poisonConsumer);

            final Map<String, Object> retryLimitHeaders = new HashMap<>();
            retryLimitHeaders.put(RABBIT_RETRY_LIMIT_HEADER, 3);
            retryLimitHeaders.put(RABBIT_RETRY_COUNT_HEADER, 3);

            final AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .headers(retryLimitHeaders)
                    .contentType("application/json")
                    .deliveryMode(2)
                    .build();

            final TaskMessage requestTaskMessage = new TaskMessage();

            final DocumentWorkerTask documentWorkerTask = new DocumentWorkerTask();
            documentWorkerTask.customData = new HashMap<>();
            documentWorkerTask.customData.put(TEST_DATA, TASK_DATA_MESSAGE);
            requestTaskMessage.setTaskId(Integer.toString(TASK_NUMBER));
            requestTaskMessage.setTaskClassifier(TEST_WORKER_NAME);
            requestTaskMessage.setTaskApiVersion(TASK_NUMBER);
            requestTaskMessage.setTaskStatus(TaskStatus.NEW_TASK);
            requestTaskMessage.setTaskData(codec.serialise(documentWorkerTask));
            requestTaskMessage.setTo(WORKER_IN);

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

            Assert.assertNotNull(poisonConsumer.getLastDeliveredBody());
            final TaskMessage decodedBody = codec.deserialise(poisonConsumer.getLastDeliveredBody(), TaskMessage.class);
            final String taskData = new String(decodedBody.getTaskData(), StandardCharsets.UTF_8);

            Assert.assertTrue(taskData.contains(POISON_ERROR_MESSAGE));
            Assert.assertTrue(taskData.contains(WORKER_FRIENDLY_NAME));
        }
    }

    private static class TestWorkerQueueConsumer implements Consumer {
        private byte[] lastDeliveredBody = null;
        @Override
        public void handleConsumeOk(String consumerTag) {

        }

        @Override
        public void handleCancelOk(String consumerTag) {

        }

        @Override
        public void handleCancel(String consumerTag) throws IOException {

        }

        @Override
        public void handleShutdownSignal(String consumerTag, ShutdownSignalException sig) {

        }

        @Override
        public void handleRecoverOk(String consumerTag) {

        }

        public byte[] getLastDeliveredBody() {
            return lastDeliveredBody;
        }

        @Override
        public void handleDelivery(final String consumerTag, final Envelope envelope, final AMQP.BasicProperties properties,
                                   final byte[] body) throws IOException {
                lastDeliveredBody = body;
        }
    }
}
