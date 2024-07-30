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

import com.hpe.caf.api.worker.*;
import com.hpe.caf.configs.SQSConfiguration;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.testng.AssertJUnit.fail;

public class SQSWorkerQueueIT {

    private static TaskCallback callback;
    private static SQSWorkerQueueConfiguration sqsWorkerQueueConfiguration;
    private static SQSConfiguration sqsConfiguration;
    private static SqsClient sqsClient;
    private static SQSWorkerQueue sqsWorkerQueue;

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

        sqsConfiguration = new SQSConfiguration();
        sqsConfiguration.setSqsProtocol("http");
        sqsConfiguration.setSqsHost("localhost");
        sqsConfiguration.setSqsPort(19324);
        sqsConfiguration.setSqsRegion("us-east-1");
        sqsConfiguration.setSqsAccessKey("x");
        sqsConfiguration.setSqsSecretAccessKey("x");

        sqsWorkerQueueConfiguration = new SQSWorkerQueueConfiguration();
        sqsWorkerQueueConfiguration.setSQSConfiguration(sqsConfiguration);
        sqsWorkerQueueConfiguration.setInputQueue("worker-in");
        sqsWorkerQueueConfiguration.setRetryQueue("retry-in");

        sqsClient = SqsClient.builder()
                    .endpointOverride(new URI(sqsConfiguration.getURIString()))
                    .region(Region.of(sqsConfiguration.getSqsRegion()))
                    .credentialsProvider(() -> new AwsCredentials() {
                        @Override
                        public String accessKeyId() {
                            return sqsConfiguration.getSqsAccessKey();
                        }

                        @Override
                        public String secretAccessKey() {
                            return sqsConfiguration.getSqsSecretAccessKey();
                        }
                    })
                    .build();

        sqsWorkerQueue = new SQSWorkerQueue(sqsWorkerQueueConfiguration);
        sqsWorkerQueue.start(callback);
    }

    @Test
    public void testInputQueueIsCreated() {
        try {
            final GetQueueUrlRequest getQueueUrlRequest = GetQueueUrlRequest.builder()
                    .queueName(sqsWorkerQueueConfiguration.getInputQueue())
                    .build();
            sqsClient.getQueueUrl(getQueueUrlRequest);
        } catch (final Exception e) {
            fail("The input queue was not created");
        }
    }

    @Test
    public void testRetryQueueIsCreated() {
        try {
            final GetQueueUrlRequest getQueueUrlRequest = GetQueueUrlRequest.builder()
                    .queueName(sqsWorkerQueueConfiguration.getRetryQueue())
                    .build();
            sqsClient.getQueueUrl(getQueueUrlRequest);
        } catch (final Exception e) {
            fail("The retry queue was not created");
        }
    }

    @Test
    public void testPublishToAnExistingQueueDoesNotThrowException() throws Exception {
        try {
            sendMessage(sqsWorkerQueue, sqsWorkerQueueConfiguration.getInputQueue(), "Hello-World");
        } catch (final Exception e) {
            fail("No exception should have been thrown");
        }
    }

    @Test
    public void testPublish() throws Exception {
        var publishQueue = "publish-queue";
        sendMessage(sqsWorkerQueue, publishQueue, "Hello-World");
        Thread.sleep(5000);
        final var receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(sqsWorkerQueue.getDeclaredQueues().get(publishQueue))
                .build();
        final var messages = sqsClient.receiveMessage(receiveRequest).messages();
        Assert.assertEquals(messages.size(), 1);
        var msg = messages.get(0).body();
        Assert.assertEquals(msg, "Hello-World");
    }

    public static void sendMessage(final SQSWorkerQueue sqsWorkerQueue, final String queueUrl, final String message) {
        try {
            var taskInfo = new TaskInformation() {

                @Override
                public String getInboundMessageId() {
                    return "XXX";
                }

                @Override
                public boolean isPoison() {
                    return false;
                }
            };

            sqsWorkerQueue.publish(taskInfo, message.getBytes(StandardCharsets.UTF_8), queueUrl, null);
        } catch (final Exception e) {
            fail(e.getMessage());
        }
    }
}
