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
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.Envelope;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Base64;
import java.util.concurrent.TimeoutException;

public class AdjustRetryLimitIT extends TestWorkerTestBase {
    private static final String POISON_ERROR_MESSAGE = "could not process the item.";
    private static final String TEST_WORKER_RESULT = "TestWorkerResult";

    // Test to confirm that changing the retry limit to below the current retry count will throw a poison message as expected.
    @Test
    public void getWorkerNameInPoisonMessageTest() throws IOException, TimeoutException {
        try(final Connection connection = connectionFactory.newConnection()) {

            final Channel channel = connection.createChannel();

            channel.queueDeclare("testworker-out", true, false, false, Collections.emptyMap());

            final TestWorkerQueueConsumer poisonConsumer = new TestWorkerQueueConsumer();
            channel.basicConsume("testworker-out", true, poisonConsumer);

            final Map<String, Object> belowRetryLimitHeaders = new HashMap<>();
            belowRetryLimitHeaders.put("x-caf-worker-retry-limit", 10);
            belowRetryLimitHeaders.put("x-caf-worker-retry", 2);

            final Map<String, Object> aboveRetryLimitHeaders = new HashMap<>();
            aboveRetryLimitHeaders.put("x-caf-worker-retry-limit", 2);
            aboveRetryLimitHeaders.put("x-caf-worker-retry", 3);

            final AMQP.BasicProperties belowRetryLimitProperties = new AMQP.BasicProperties.Builder()
                    .headers(belowRetryLimitHeaders)
                    .contentType("application/json")
                    .deliveryMode(2)
                    .priority(1)
                    .build();

            final AMQP.BasicProperties aboveRetryLimitProperties = new AMQP.BasicProperties.Builder()
                    .headers(aboveRetryLimitHeaders)
                    .contentType("application/json")
                    .deliveryMode(2)
                    .priority(1)
                    .build();

            final String body = "{\"version\":1,\"taskId\":\"1.1\",\"taskClassifier\":\"testWorkerIdentifier\",\"taskApiVersion\":1," +
                    "\"taskData\":\"cG9pc29uIG1lc3NhZ2U\",\"taskStatus\":\"RESULT_SUCCESS\",\"context\":{},\"to\":\"worker-in\"," +
                    "\"tracking\":null,\"sourceInfo\":{\"name\":\"worker-test\",\"version\":\"1.0.0\"}}";

            channel.basicPublish("", "worker-in", belowRetryLimitProperties, body.getBytes(StandardCharsets.UTF_8));
            channel.basicPublish("", "worker-in", aboveRetryLimitProperties, body.getBytes(StandardCharsets.UTF_8));

            try {
                for (int i=0; i<100; i++){

                    Thread.sleep(100);

                    if (poisonConsumer.getResponseTaskData().size() == 2){
                        break;
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            Assert.assertNotNull(poisonConsumer.getResponseTaskData());

            final ArrayList<String> messageResponses = poisonConsumer.getResponseTaskData();

            Assert.assertTrue(messageResponses.get(0).contains(TEST_WORKER_RESULT));
            Assert.assertTrue(messageResponses.get(1).contains(POISON_ERROR_MESSAGE));
        }
    }
    private static class TestWorkerQueueConsumer implements Consumer {
        private final ArrayList<String> responseBodyTaskData = new ArrayList<>();
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
        public ArrayList<String> getResponseTaskData(){ return responseBodyTaskData; }

        @Override
        public void handleDelivery(final String consumerTag, final Envelope envelope, final AMQP.BasicProperties properties,
                                   final byte[] body) throws IOException {

            responseBodyTaskData.add(
                    new String(Base64.getDecoder().decode(
                        new JSONObject(
                            new String(body, StandardCharsets.UTF_8)).getString("taskData"))));
        }
    }
}
