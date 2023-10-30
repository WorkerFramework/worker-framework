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
import com.hpe.caf.util.rabbitmq.QueueCreator;
import org.json.JSONObject;
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
import java.util.Base64;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

public class GetWorkerNameIT extends TestWorkerTestBase {
    private static final String POISON_ERROR_MESSAGE = "could not process the item.";
    private static final String WORKER_FRIENDLY_NAME = "TestWorker";
    private static final String RABBIT_PROP_QUEUE_TYPE_NAME = System.getenv("RABBIT_PROP_QUEUE_TYPE_NAME");

    @Test
    public void getWorkerNameInPoisonMessageTest() throws IOException, TimeoutException {

        try(final Connection connection = connectionFactory.newConnection()) {

            final Channel channel = connection.createChannel();

            Map<String, Object> args = new HashMap<>();
            final String rabbitQueueTypeName = !Strings.isNullOrEmpty(RABBIT_PROP_QUEUE_TYPE_NAME) ?
                    RABBIT_PROP_QUEUE_TYPE_NAME : QueueCreator.RABBIT_PROP_QUEUE_TYPE_QUORUM;
            args.put(QueueCreator.RABBIT_PROP_QUEUE_TYPE, rabbitQueueTypeName);

            channel.queueDeclare("testworker-out", true, false, false, args);

            final TestWorkerQueueConsumer poisonConsumer = new TestWorkerQueueConsumer();
            channel.basicConsume("testworker-out", true, poisonConsumer);

            final Map<String, Object> retryLimitHeaders = new HashMap<>();
            retryLimitHeaders.put("x-caf-worker-retry-limit", 3);
            retryLimitHeaders.put("x-caf-worker-retry", 3);

            final AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .headers(retryLimitHeaders)
                    .contentType("application/json")
                    .deliveryMode(2)
                    .build();

            final String body = "{\"version\":1,\"taskId\":\"1.1\",\"taskClassifier\":\"testWorkerIdentifier\",\"taskApiVersion\":1," +
                    "\"taskData\":\"cG9pc29uIG1lc3NhZ2U\",\"taskStatus\":\"RESULT_SUCCESS\",\"context\":{},\"to\":\"worker-in\"," +
                    "\"tracking\":null,\"sourceInfo\":{\"name\":\"worker-test\",\"version\":\"1.0.0\"}}";

            channel.basicPublish("", "worker-in", properties, body.getBytes(StandardCharsets.UTF_8));

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
            final String returnedBody = new String(poisonConsumer.getLastDeliveredBody(), StandardCharsets.UTF_8);
            final JSONObject obj = new JSONObject(returnedBody);
            final byte[] taskData = Base64.getDecoder().decode(obj.getString("taskData"));

            final String decodedTaskData = new String(taskData);


            Assert.assertTrue(decodedTaskData.contains(POISON_ERROR_MESSAGE));
            Assert.assertTrue(decodedTaskData.contains(WORKER_FRIENDLY_NAME));
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
