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

import com.google.common.collect.Iterables;
import com.hpe.caf.worker.queue.sqs.config.SQSWorkerQueueConfiguration;
import com.hpe.caf.worker.queue.sqs.publisher.error.PublishError;
import com.hpe.caf.worker.queue.sqs.publisher.message.WorkerMessage;
import com.hpe.caf.worker.queue.sqs.publisher.response.PublishBatchResponse;
import com.hpe.caf.worker.queue.sqs.visibility.VisibilityMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.hpe.caf.worker.queue.sqs.util.SQSUtil.MAX_MESSAGE_BATCH_SIZE;

/**
 * This class ...
 */
public class WorkerPublisher implements Runnable
{
    private static final Logger LOG = LoggerFactory.getLogger(WorkerPublisher.class);

    private final SqsClient sqsClient;
    private final SQSWorkerQueueConfiguration queueCfg;
    private final VisibilityMonitor visibilityMonitor;
    private final Map<String, List<WorkerMessage>> publishCollections;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public WorkerPublisher(
            final SqsClient sqsClient,
            final SQSWorkerQueueConfiguration queueCfg,
            final VisibilityMonitor visibilityMonitor)
    {
        this.sqsClient = sqsClient;
        this.queueCfg = queueCfg;
        this.visibilityMonitor = visibilityMonitor;
        this.publishCollections = new HashMap<>();
    }

    @Override
    public void run()
    {
        while (running.get()) {
            try {
                send();
                Thread.sleep(queueCfg.getPublisherWaitTimeout() * 1000);
            } catch (final InterruptedException e) {
                LOG.error("A pause in task deletion was interrupted", e);
            }
        }
    }

    /**
     * Publishes messages for multiple destinations
     * @throws InterruptedException
     */
    private void send() throws InterruptedException
    {
        for(final var entry : publishCollections.entrySet()) {
            final var publishList = entry.getValue();
            final var queueUrl = entry.getKey();
            synchronized (publishList) {
                if (!publishList.isEmpty()) {
                    final var publishErrors = sendWorkerMessages(queueUrl, publishList);
                    publishList.clear();
                    if (!publishErrors.isEmpty()) {
                        final var taskInfos = publishErrors.stream()
                                .map(pe -> pe.workerMessage().getInboundTaskInfo())
                                .toList();
                        visibilityMonitor.unwatch(taskInfos);
                    }
                }
            }
        }
    }

    private List<PublishError> sendWorkerMessages(
            final String queueUrl,
            final List<WorkerMessage> publishList
    )
    {
        int publishCount = 0;
        final var publishErrors = new ArrayList<PublishError>();
        final var batches = Iterables.partition(publishList, MAX_MESSAGE_BATCH_SIZE);
        for(final var batch : batches) {
            final var response = sendBatch(queueUrl, batch);
            publishErrors.addAll(response.errors());
            publishCount += response.successes();
        }
        LOG.info("Published {} message(s) to queue {}", publishCount, queueUrl);
        return publishErrors;
    }

    private PublishBatchResponse sendBatch(
            final String queueUrl,
            final List<WorkerMessage> messages
    )
    {
        final Map<String, WorkerMessage> workerMessageMap = new HashMap<>();
        final var entries = new ArrayList<SendMessageBatchRequestEntry>();
        int id = 1;
        for(final WorkerMessage msg : messages) {
            final var idStr = String.valueOf(id++);
            entries.add(SendMessageBatchRequestEntry.builder()
                    .id(idStr)
                    .messageAttributes(createAttributesFromMessageHeaders(msg.getHeaders()))
                    .messageBody(new String(msg.getTaskMessage(), StandardCharsets.UTF_8))
                    .build());
            workerMessageMap.put(idStr, msg);
        }
        final var request = SendMessageBatchRequest.builder()
                .entries(entries)
                .queueUrl(queueUrl)
                .build();

        final var response = sqsClient.sendMessageBatch(request);

        final var succeeded = response.successful();
        LOG.debug("Sent {} message(s) to queue {}", succeeded.size(), queueUrl);
        final var failed = response.failed();

        final var publishErrors = failed.stream()
                .map(f -> new PublishError(f.message(), queueUrl, workerMessageMap.get(f.id())))
                .collect(Collectors.toList());
        return new PublishBatchResponse(succeeded.size(), publishErrors);
    }

    public void publish(final WorkerMessage workerMessage)
    {
        final var queueInfo = workerMessage.getQueueInfo();
        final var queueMessages = publishCollections.computeIfAbsent(
                queueInfo.url(),
                (q) -> Collections.synchronizedList(new ArrayList<>())
        );
        queueMessages.add(workerMessage);
    }

    public void shutdown()
    {
        running.set(false);
    }

    private static Map<String, MessageAttributeValue> createAttributesFromMessageHeaders(final Map<String, Object> headers)
    {
        final var attributes = new HashMap<String, MessageAttributeValue>();
        for(final Map.Entry<String, Object> entry : headers.entrySet()) {
            attributes.put(entry.getKey(), MessageAttributeValue.builder()
                    .dataType("String")
                    .stringValue(entry.getValue().toString())
                    .build());
        }
        return attributes;
    }
}
