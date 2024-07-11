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
package com.hpe.caf.worker.queue.sqs;

import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.QueueException;
import com.hpe.caf.api.worker.TaskCallback;
import com.hpe.caf.api.worker.TaskInformation;
import com.hpe.caf.api.worker.TaskRejectedException;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.Map;

public class SQSWorkerQueueIT {

    private static TaskCallback callback;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        callback = new TaskCallback() {
            @Override
            public void registerNewTask(TaskInformation taskInformation, byte[] taskData, Map<String, Object> headers) throws TaskRejectedException, InvalidTaskException {

            }

            @Override
            public void abortTasks() {

            }
        };
    }

    @Test
    public void testThatQueuesAreDeclaredAndReceiveMessages() throws QueueException, InterruptedException {
        final var sqsWorkerQueue = new SQSWorkerQueue();
        sqsWorkerQueue.start(callback);
        final var queueUrl = sqsWorkerQueue.createQueue("worker-in");
        System.out.println("QURL:" + queueUrl);
        final var client = sqsWorkerQueue.getSqsClient();
        sendMessage(client, queueUrl, "Hello-World");
        Thread.sleep(10000);
        final var receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .build();
        final var messages = client.receiveMessage(receiveRequest).messages();
        Assert.assertEquals(messages.size(), 1);
        var msg = messages.get(0).body();
        System.out.println("MESSAGE READ:" + msg);
        Assert.assertEquals(msg, "Hello-World");
    }

    public static void sendMessage(final SqsClient sqsClient, final String queueUrl, final String message) {
        try {
            final var sendMsgRequest = SendMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .messageBody(message)
                    .delaySeconds(5)
                    .build();

            sqsClient.sendMessage(sendMsgRequest);
        } catch (SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }
}