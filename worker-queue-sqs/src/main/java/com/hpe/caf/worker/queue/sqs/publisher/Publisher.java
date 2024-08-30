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
package com.hpe.caf.worker.queue.sqs.publisher;

import com.hpe.caf.worker.queue.sqs.metrics.MetricsReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;
import software.amazon.awssdk.services.sqs.model.ReceiptHandleIsInvalidException;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsRequest;

import java.util.concurrent.BlockingQueue;

public class Publisher implements Runnable
{
    private final SqsClient sqsClient;
    private final MetricsReporter metricsReporter;
    private final BlockingQueue<PublishEvent> publisherQueue;

    private static final Logger LOG = LoggerFactory.getLogger(Publisher.class);

    public Publisher(
            final SqsClient sqsClient,
            final MetricsReporter metricsReporter,
            final BlockingQueue<PublishEvent> publisherQueue
    )
    {
        this.sqsClient = sqsClient;
        this.metricsReporter = metricsReporter;
        this.publisherQueue = publisherQueue;
    }

    @Override
    public void run()
    {
        publish();
    }

    private void publish()
    {
        while (true) {
            try {
                final var event = publisherQueue.take();
                final var request = event.request();
                if (request instanceof SendMessageRequest) {
                    sqsClient.sendMessage((SendMessageRequest) request);
                    metricsReporter.incrementPublished();
                }

                if (request instanceof DeleteMessageRequest) {
                    sqsClient.deleteMessage((DeleteMessageRequest) request);
                }
            } catch (InterruptedException e) {
                LOG.error("Publishing Interrupted", e);
                metricsReporter.incrementErrors();
            }
        }
    }
}
