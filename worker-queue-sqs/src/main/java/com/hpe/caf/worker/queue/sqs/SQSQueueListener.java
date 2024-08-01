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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.util.concurrent.BlockingQueue;

public class SQSQueueListener implements Runnable
{
    private final BlockingQueue<Message> consumerQueue;
    private final SqsClient sqsClient;
    private final String queueUrl;

    private static final Logger LOG = LoggerFactory.getLogger(SQSQueueListener.class);

    public SQSQueueListener(
            final BlockingQueue<Message> consumerQueue,
            final SqsClient sqsClient,
            final String queueUrl)
    {
        this.consumerQueue = consumerQueue;
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
    }

    @Override
    public void run()
    {
        receiveMessages();
    }

    public void receiveMessages()
    {
        while (true) {
            final var receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(1) // DDD configurable??
                    .waitTimeSeconds(20) // DDD configurable??
                    .build();
            final var receiveMessageResult = sqsClient.receiveMessage(receiveRequest).messages();
            for (final var message : receiveMessageResult) {
                LOG.debug("Received {} on queue {} ", message.body(), queueUrl);
                consumerQueue.add(message);
            }
        }
    }
}
