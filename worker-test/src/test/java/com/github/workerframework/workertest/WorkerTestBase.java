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
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.ShutdownSignalException;
import org.testng.Assert;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.github.workerframework.util.rabbitmq.RabbitHeaders.RABBIT_HEADER_CAF_PAYLOAD_OFFLOADING_STORAGE_REF;

public class WorkerTestBase {
    final protected ConnectionFactory connectionFactory;
    private static final String CAF_RABBITMQ_HOST = "CAF_RABBITMQ_HOST";
    private static final String CAF_RABBITMQ_PORT = "CAF_RABBITMQ_PORT";
    private static final String CAF_RABBITMQ_USERNAME = "CAF_RABBITMQ_USERNAME";
    private static final String CAF_RABBITMQ_PASSWORD = "CAF_RABBITMQ_PASSWORD";
    private static final String WEBDAV_URL = System.getProperty("WEBDAV_URL", "http://localhost:9090/webdav");
    protected static final Codec codec = new JsonCodec();

    public WorkerTestBase() {
        connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(System.getProperty(CAF_RABBITMQ_HOST, "localhost"));
        connectionFactory.setPort(Integer.parseInt(System.getProperty(CAF_RABBITMQ_PORT, "25672")));
        connectionFactory.setUsername(System.getProperty(CAF_RABBITMQ_USERNAME, "guest"));
        connectionFactory.setPassword(System.getProperty(CAF_RABBITMQ_PASSWORD, "guest"));
        connectionFactory.setVirtualHost("/");
    }

    public void publish(
        final Channel channel,
        final byte[] taskMessage,
        final Map<String, Object> headers,
        final String workerIn
    ) throws IOException {
        final AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
            .contentType("application/json")
            .deliveryMode(2)
            .headers(headers)
            .build();

        channel.basicPublish("", workerIn, properties, taskMessage);
    }

    public void consume(
        final Channel channel,
        final TestWorkerQueueConsumer messageConsumer,
        final String workerOut
    ) throws IOException {
        channel.basicConsume(workerOut, false, messageConsumer);
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

    public TaskMessage getTaskMessage(
        final String testWorkerName,
        final int taskNumber,
        final TestWorkerTask documentWorkerTask,
        final String workerIn
    ) throws CodecException {
        final var trackingInfo = new TrackingInfo(testWorkerName + taskNumber, new Date(), 1, null, "pipe", "to");
        final TaskMessage requestTaskMessage = new TaskMessage();
        requestTaskMessage.setTaskId(Integer.toString(taskNumber));
        requestTaskMessage.setTaskClassifier(testWorkerName);
        requestTaskMessage.setTaskApiVersion(1);
        requestTaskMessage.setTaskStatus(TaskStatus.NEW_TASK);
        requestTaskMessage.setTaskData(codec.serialise(documentWorkerTask));
        requestTaskMessage.setTo(workerIn);
        requestTaskMessage.setTracking(trackingInfo);
        return requestTaskMessage;
    }

    public Channel prepareChannel(final Connection connection, final String workerIn, final String workerOut, 
                                  final String workerInvalid) throws IOException 
    {
        final Channel channel = connection.createChannel();
        final Map<String, Object> args = new HashMap<>();
        args.put(QueueCreator.RABBIT_PROP_QUEUE_TYPE, QueueCreator.RABBIT_PROP_QUEUE_TYPE_QUORUM);
        channel.queueDeclare(workerIn, true, false, false, args);
        channel.queueDeclare(workerOut, true, false, false, args);
        channel.queueDeclare(workerInvalid, true, false, false, args);
        return channel;
    }

    /**
     * This method will return the storage ref of the offloaded payload stored in the datastore on publish to the 
     * worker-out queue.
     * @param messageConsumer
     * @return
     */
    public static Optional<String> getTaskMessageStorageRef(final TestWorkerQueueConsumer messageConsumer) {
        final Map<String, Object> outgoingHeaders = messageConsumer.getHeaders();
        final Optional<String> outgoingTaskMessageStorageRef = outgoingHeaders.containsKey(RABBIT_HEADER_CAF_PAYLOAD_OFFLOADING_STORAGE_REF) ?
            Optional.of(outgoingHeaders.get(RABBIT_HEADER_CAF_PAYLOAD_OFFLOADING_STORAGE_REF).toString()) :
            Optional.empty();
        return outgoingTaskMessageStorageRef;
    }
    
    public static Optional<byte[]> readFileFromWebDAV(final String messageStorageRef) throws Exception {
        final String fileUrl = String.format("%s/%s", WEBDAV_URL, messageStorageRef);
        final URL url = new URL(fileUrl);
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        
        if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
            return Optional.empty();
        }
        
        try (InputStream in = conn.getInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            final byte[] buffer = new byte[4096];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return Optional.of(out.toByteArray());
        }
    }

    public static void writeFileToWebDav(final String messageStorageRef, byte[] fileData) throws Exception {
        final String fileUrl = String.format("%s/%s", WEBDAV_URL, messageStorageRef);
        final URL url = new URL(fileUrl);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("PUT");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/octet-stream");

        try (OutputStream out = conn.getOutputStream()) {
            out.write(fileData);
        }

        final int responseCode = conn.getResponseCode();
        if (responseCode != HttpURLConnection.HTTP_CREATED && responseCode != HttpURLConnection.HTTP_NO_CONTENT) {
            Assert.fail("Failed to write file. HTTP response code: " + responseCode);
        }
    }

    public static class TestWorkerQueueConsumer implements Consumer {
        private byte[] lastDeliveredBody = null;
        private Map<String, Object> headers = null;
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

        @Override
        public void handleDelivery(final String consumerTag, final Envelope envelope, final AMQP.BasicProperties properties,
                                   final byte[] body) throws IOException {
            lastDeliveredBody = body;
            headers = properties.getHeaders();
        }
    }
}
