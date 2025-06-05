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

import com.github.workerframework.api.TaskMessage;
import com.github.workerframework.testworker.TestWorkerTask;
import com.github.workerframework.util.rabbitmq.RabbitHeaders;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.github.workerframework.util.rabbitmq.RabbitHeaders.RABBIT_HEADER_CAF_PAYLOAD_OFFLOADING_STORAGE_REF;

public class PayloadOffloadingIT extends WorkerTestBase {
    private static final String TEST_WORKER_NAME = "PayloadOffloadingIT";
    private static final String WORKER_IN = "PayloadOffloadingIT-in";
    private static final String WORKER_OUT = "PayloadOffloadingIT-out";
    private static final String WORKER_INVALID = "PayloadOffloadingIT-invalid";

    private static final String TERMINAL_WORKER_IN = "PayloadOffloadingIT-Terminal-in";
    private static final String TERMINAL_WORKER_OUT = "PayloadOffloadingIT-Terminal-out";
    private static final String TERMINAL_WORKER_INVALID = "PayloadOffloadingIT-Terminal-invalid";

    @Test
    public void checkOffloadedPayloadIsConsumedAndDeletedOnAck() throws Exception {
        final TestWorkerTask documentWorkerTask = new TestWorkerTask();
        documentWorkerTask.setPoison(false);
        
        final TaskMessage taskMessage = getTaskMessage(TEST_WORKER_NAME, 1, documentWorkerTask, WORKER_IN);
        final byte[] taskData = taskMessage.getTaskData();
        taskMessage.setTaskData(null);
        
        final var setupPayloadOffloadStorageRef = UUID.randomUUID().toString();
        writeFileToWebDav(setupPayloadOffloadStorageRef, taskData);
        final var readWebDAVFile = readFileFromWebDAV(setupPayloadOffloadStorageRef);
        Assert.assertTrue(readWebDAVFile.isPresent(), "The file should be present in the datastore");

        try(final Connection connection = connectionFactory.newConnection();
            final Channel channel = prepareChannel(connection, WORKER_IN, WORKER_OUT, WORKER_INVALID)) {

            //  Now we can send a message which expects to find the setupPayloadOffloadStorageRef.
            final Map<String, Object> headers = new HashMap<>();
            headers.put(RABBIT_HEADER_CAF_PAYLOAD_OFFLOADING_STORAGE_REF, setupPayloadOffloadStorageRef);
            // this publish will result in an offloaded payload being recovered by the consumer.
            // the body will be ignored as the offloaded payload will be used.
            publish(channel, codec.serialise(taskMessage), headers, WORKER_IN);

            final TestWorkerQueueConsumer outboundConsumer = new TestWorkerQueueConsumer();
            consume(channel, outboundConsumer, WORKER_OUT);
            final var consumedTaskMessageStorageRef = getTaskMessageStorageRef(outboundConsumer);
            Assert.assertTrue(consumedTaskMessageStorageRef.isPresent(), "The payload offloading header was missing");
            Assert.assertNotEquals(consumedTaskMessageStorageRef.get(), setupPayloadOffloadStorageRef, "Storage refs should have been different");
            Assert.assertTrue(consumedTaskMessageStorageRef.get().contains(WORKER_OUT), "The storage reference does not contain the output queue name");

            // The previously offloaded payload should now have been deleted when the inbound message is ack'd
            final var storedSetupByteArrayOpt = readFileFromWebDAV(setupPayloadOffloadStorageRef);
            Assert.assertTrue(storedSetupByteArrayOpt.isEmpty(), "setup message should not have been found");

            // The outbound message should be present in the datastore
            final var consumedByteArrayOpt = readFileFromWebDAV(consumedTaskMessageStorageRef.get());
            Assert.assertTrue(consumedByteArrayOpt.isPresent(), "Offloaded payload should have been found");
            Assert.assertEquals(new String(consumedByteArrayOpt.get(), StandardCharsets.UTF_8), "TestWorkerResultTaskData");
        }
    }

    @Test
    public void invalidOffloadedPayloadReference() throws Exception {
        final TestWorkerTask documentWorkerTask = new TestWorkerTask();
        documentWorkerTask.setPoison(false);

        final TaskMessage taskMessage = getTaskMessage(TEST_WORKER_NAME, 1, documentWorkerTask, WORKER_IN);
        taskMessage.setTaskData(null);


        try(final Connection connection = connectionFactory.newConnection();
            final Channel channel = prepareChannel(connection, WORKER_IN, WORKER_OUT, WORKER_INVALID)) {

            //  Now we can send a message which expects to find the setupPayloadOffloadStorageRef.
            final Map<String, Object> headers = new HashMap<>();
            final String invalidReference = UUID.randomUUID().toString();
            headers.put(RABBIT_HEADER_CAF_PAYLOAD_OFFLOADING_STORAGE_REF, invalidReference);
            publish(channel, codec.serialise(taskMessage), headers, WORKER_IN);

            final TestWorkerQueueConsumer outboundConsumer = new TestWorkerQueueConsumer();
            consume(channel, outboundConsumer, WORKER_OUT);
            Assert.assertEquals(
                    outboundConsumer.getHeaders().get(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_INVALID).toString(),
                    "Reference not found: " + invalidReference);

            Assert.assertEquals(
                    outboundConsumer.getHeaders().get(RabbitHeaders.RABBIT_HEADER_CAF_PAYLOAD_OFFLOADING_STORAGE_REF).toString(),
                    invalidReference);
            
        }
    }

    @Test
    public void invalidOffloadedPayload() throws Exception {
        final TestWorkerTask documentWorkerTask = new TestWorkerTask();
        documentWorkerTask.setPoison(false);

        final TaskMessage taskMessage = getTaskMessage(TEST_WORKER_NAME, 1, documentWorkerTask, WORKER_IN);
        taskMessage.setTaskData(null);
        final var setupPayloadOffloadStorageRef = UUID.randomUUID().toString();
        writeFileToWebDav(setupPayloadOffloadStorageRef, "Junk data not JSON".getBytes(StandardCharsets.UTF_8));
        
        try(final Connection connection = connectionFactory.newConnection();
            final Channel channel = prepareChannel(connection, WORKER_IN, WORKER_OUT, WORKER_INVALID)) {

            //  Now we can send a message which expects to find the setupPayloadOffloadStorageRef.
            final Map<String, Object> headers = new HashMap<>();
            headers.put(RABBIT_HEADER_CAF_PAYLOAD_OFFLOADING_STORAGE_REF, setupPayloadOffloadStorageRef);
            publish(channel, codec.serialise(taskMessage), headers, WORKER_IN);

            final TestWorkerQueueConsumer outboundConsumer = new TestWorkerQueueConsumer();
            consume(channel, outboundConsumer, WORKER_OUT);
            Assert.assertEquals(
                    outboundConsumer.getHeaders().get(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_INVALID).toString(),
                    "");

            Assert.assertEquals(
                    outboundConsumer.getHeaders().get(RabbitHeaders.RABBIT_HEADER_CAF_PAYLOAD_OFFLOADING_STORAGE_REF).toString(),
                    setupPayloadOffloadStorageRef);

        }
    }
    
    @Test
    public void checkOffloadedPayloadIsDeletedOnTerminalWorker() throws Exception {
        // First we need a message stored in the datastore
        final TestWorkerTask terminalDocumentWorkerTask = new TestWorkerTask();
        terminalDocumentWorkerTask.setTerminalWorker(true);

        final TaskMessage taskMessage = getTaskMessage(TEST_WORKER_NAME, 2, terminalDocumentWorkerTask, 
                TERMINAL_WORKER_IN);
        final byte[] taskData = taskMessage.getTaskData();
        taskMessage.setTaskData(null);

        final var storageRef = UUID.randomUUID().toString();
        writeFileToWebDav(storageRef, taskData);
        final var readWebDAVFile = readFileFromWebDAV(storageRef);
        Assert.assertTrue(readWebDAVFile.isPresent(), "The file should be present in the datastore");

        try(final Connection connection = connectionFactory.newConnection();
            final Channel channel = prepareChannel(connection, TERMINAL_WORKER_IN, TERMINAL_WORKER_OUT, 
                    TERMINAL_WORKER_INVALID)) {

            //  Now we can send a message which expects to find the taskMessageStorageRef.
            final Map<String, Object> headers = new HashMap<>();
            headers.put(RABBIT_HEADER_CAF_PAYLOAD_OFFLOADING_STORAGE_REF, storageRef);

            // this publish will result the worker thinking it a terminal worker.
            publish(channel,codec.serialise(taskMessage), headers, TERMINAL_WORKER_IN);

            final TestWorkerQueueConsumer outboundConsumer = new TestWorkerQueueConsumer();
            consume(channel, outboundConsumer, TERMINAL_WORKER_OUT);
            try {
                for (int i=0; i<100; i++){
                    Thread.sleep(100);
                    if (outboundConsumer.getLastDeliveredBody() != null){
                        break;
                    }
                }
            } catch (final InterruptedException e) {
                throw new RuntimeException(e);
            }
            Assert.assertNull(outboundConsumer.getLastDeliveredBody(), "The message should not have been output to the queue");

            final var reReadWebDAVFile = readFileFromWebDAV(storageRef);
            Assert.assertFalse(reReadWebDAVFile.isPresent(), "The file should be gone from the datastore");
        }
    }
    
}
