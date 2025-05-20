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

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import org.testng.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

import static com.github.workerframework.util.rabbitmq.RabbitHeaders.RABBIT_HEADER_CAF_PAYLOAD_OFFLOADING_STORAGE_REF;

public class WorkerTestBase {
    final protected ConnectionFactory connectionFactory;
    private static final String CAF_RABBITMQ_HOST = "CAF_RABBITMQ_HOST";
    private static final String CAF_RABBITMQ_PORT = "CAF_RABBITMQ_PORT";
    private static final String CAF_RABBITMQ_USERNAME = "CAF_RABBITMQ_USERNAME";
    private static final String CAF_RABBITMQ_PASSWORD = "CAF_RABBITMQ_PASSWORD";
    public static final String WEBDAV_URL = System.getProperty("WEBDAV_URL");

    public WorkerTestBase() {
        connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(System.getProperty(CAF_RABBITMQ_HOST));
        connectionFactory.setPort(Integer.parseInt(System.getProperty(CAF_RABBITMQ_PORT)));
        connectionFactory.setUsername(System.getProperty(CAF_RABBITMQ_USERNAME));
        connectionFactory.setPassword(System.getProperty(CAF_RABBITMQ_PASSWORD));
        connectionFactory.setVirtualHost("/");
    }

    /**
     * This method will return the storage ref of the offloaded payload stored in the datastore on publish to the 
     * worker-out queue.
     * @param messageConsumer
     * @return
     */
    public static String getTaskMessageStorageRef(final TestWorkerQueueConsumer messageConsumer) {
        final Map<String, Object> outgoingHeaders = messageConsumer.getHeaders();
        final Optional<String> outgoingTaskMessageStorageRef = outgoingHeaders.containsKey(RABBIT_HEADER_CAF_PAYLOAD_OFFLOADING_STORAGE_REF) ?
            Optional.of(outgoingHeaders.get(RABBIT_HEADER_CAF_PAYLOAD_OFFLOADING_STORAGE_REF).toString()) :
            Optional.empty();
        Assert.assertTrue(outgoingTaskMessageStorageRef.isPresent(), "The payload offloading header was missing");
        return outgoingTaskMessageStorageRef.get();
    }
    
    public static Optional<byte[]> readFileFromWebDAV(final String messageStorageRef) throws Exception {
        final String fileUrl = String.format("%s/%s", WEBDAV_URL, messageStorageRef);
        URL url = new URL(fileUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            return Optional.empty();
        }
        
        try (InputStream in = conn.getInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return Optional.of(out.toByteArray());
        }
    }

    public static class TestWorkerQueueConsumer implements Consumer {
        private byte[] lastDeliveredBody = null;
        private Map<String, Object> headers = null;
        private Envelope envelope = null;
        
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

        public Map<String, Object> getHeaders() {
            return headers;
        }

        public Envelope getEnvelope() {
            return envelope;
        }

        @Override
        public void handleDelivery(final String consumerTag, final Envelope envelope, final AMQP.BasicProperties properties,
                                   final byte[] body) throws IOException {
            this.envelope = envelope;
            lastDeliveredBody = body;
            headers = properties.getHeaders();
        }
    }
}
