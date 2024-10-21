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
import com.hpe.caf.util.rabbitmq.RabbitHeaders;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class ShutdownDeveloperTest extends TestWorkerTestBase {
    private static final String TEST_WORKER_NAME = "testWorkerIdentifier";
    private static final String WORKER_IN = "worker-in";
    private static final String TESTWORKER_OUT = "testworker-out";
    private static final int TASK_NUMBER = 1;
    private static final Codec codec = new JsonCodec();

    @Test
    @Ignore 
    public void shutdownTest() throws IOException, TimeoutException, CodecException {

        // Usage instructions
        // Comment out the iages for test worker 2 and 3 in this module's pom.xml
        // Use mvn docker:start to start test worker
        // Remove the @Ignore and run the test to create 100 test messages
        // From a terminal execute docker stop -t 300 CONTAINER_ID 
        // This will issue a SIGTERM and wait for 300 seconds before issuing SIGKILL
        // Observe worker-in in the RabbitMQ Management UI 
        // The worker will stop consuming new messages and process all the pre-fetched messages
        // When all prefetched messages are ack'd the worker container will stop before 300 s grace period
        
        try(final Connection connection = connectionFactory.newConnection()) {

            final Channel channel = connection.createChannel();

            final Map<String, Object> args = new HashMap<>();
            args.put(QueueCreator.RABBIT_PROP_QUEUE_TYPE, QueueCreator.RABBIT_PROP_QUEUE_TYPE_QUORUM);

            channel.queueDeclare(TESTWORKER_OUT, true, false, false, args);
            channel.queueDeclare(WORKER_IN, true, false, false, args);
            
            final Map<String, Object> retryLimitHeaders = new HashMap<>();

            final AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .headers(retryLimitHeaders)
                    .contentType("application/json")
                    .deliveryMode(2)
                    .build();
            
            // Send 100 messages, the test worker will consume 1 message every 5 seconds.
            for(int index = 1; index <= 100; index ++) {
                final TaskMessage requestTaskMessage = new TaskMessage();

                final TestWorkerTask documentWorkerTask = new TestWorkerTask();
                documentWorkerTask.setPoison(false);
                documentWorkerTask.setDelaySeconds(5);
                requestTaskMessage.setTaskId(Integer.toString(index));
                requestTaskMessage.setTaskClassifier(TEST_WORKER_NAME);
                requestTaskMessage.setTaskApiVersion(TASK_NUMBER);
                requestTaskMessage.setTaskStatus(TaskStatus.NEW_TASK);
                requestTaskMessage.setTaskData(codec.serialise(documentWorkerTask));
                requestTaskMessage.setTo(WORKER_IN);

                channel.basicPublish("", WORKER_IN, properties, codec.serialise(requestTaskMessage));
            }
        }
    }
}
