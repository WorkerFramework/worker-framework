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

public class RetryLimitIT extends TestWorkerTestBase {
    private static final String POISON_ERROR_MESSAGE = "could not process the item.";
    private static final String TEST_WORKER_RESULT = "TestWorkerResult";

    @Test
    public void getResultSuccessIfRetryNumberLessThanRetryLimitTest() throws IOException, TimeoutException {

        final String decodedTaskData = getResponse(10,2);

        Assert.assertTrue(decodedTaskData.contains(TEST_WORKER_RESULT));
    }

    @Test
    public void getPoisonMessageIfRetryNumberGreaterThanRetryLimitTest() throws IOException, TimeoutException {

        final String decodedTaskData = getResponse(2,3);

        Assert.assertTrue(decodedTaskData.contains(POISON_ERROR_MESSAGE));

    }

    @Test
    public void getPoisonMessageIfRetryNumberEqualToRetryLimitTest() throws IOException, TimeoutException {

        final String decodedTaskData = getResponse(10,10);

        Assert.assertTrue(decodedTaskData.contains(POISON_ERROR_MESSAGE));

    }

    private String getResponse(final int retryLimit, final int retryCount) throws IOException, TimeoutException  {
        try(final Connection connection = connectionFactory.newConnection()) {

            final Channel channel = connection.createChannel();

            channel.queueDeclare("testworker-out", true, false, false, Collections.emptyMap());

            final TestWorkerQueueConsumer poisonConsumer = new TestWorkerQueueConsumer();
            channel.basicConsume("testworker-out", true, poisonConsumer);

            final Map<String, Object> aboveRetryLimitHeaders = new HashMap<>();
            aboveRetryLimitHeaders.put("x-caf-worker-retry-limit", retryLimit);
            aboveRetryLimitHeaders.put("x-caf-worker-retry", retryCount);

            final AMQP.BasicProperties aboveRetryLimitProperties = new AMQP.BasicProperties.Builder()
                    .headers(aboveRetryLimitHeaders)
                    .contentType("application/json")
                    .deliveryMode(2)
                    .priority(1)
                    .build();

            final String body = "{\"version\":1,\"taskId\":\"1.1\",\"taskClassifier\":\"testWorkerIdentifier\",\"taskApiVersion\":1," +
                    "\"taskData\":\"cG9pc29uIG1lc3NhZ2U\",\"taskStatus\":\"RESULT_SUCCESS\",\"context\":{},\"to\":\"worker-in\"," +
                    "\"tracking\":null,\"sourceInfo\":{\"name\":\"worker-test\",\"version\":\"1.0.0\"}}";

            channel.basicPublish("", "worker-in", aboveRetryLimitProperties, body.getBytes(StandardCharsets.UTF_8));

            channel.basicConsume("testworker-out", true, poisonConsumer);
            try {
                for (int i = 0; i < 100; i++) {

                    Thread.sleep(100);

                    if (poisonConsumer.getLastDeliveredBody() != null) {
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

            return new String(taskData);
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
