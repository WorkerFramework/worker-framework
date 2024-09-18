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
import com.hpe.caf.worker.queue.sqs.publisher.error.DeletionError;
import com.hpe.caf.worker.queue.sqs.publisher.message.DeleteMessage;
import com.hpe.caf.worker.queue.sqs.publisher.response.DeleteBatchResponse;
import com.hpe.caf.worker.queue.sqs.visibility.VisibilityMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.hpe.caf.worker.queue.sqs.util.SQSUtil.MAX_MESSAGE_BATCH_SIZE;

/**
 * This class monitors tasks and when complete deletes the associated messages in batches.
 */
public class DeletePublisher implements Runnable
{
    private static final Logger LOG = LoggerFactory.getLogger(DeletePublisher.class);

    private final SqsClient sqsClient;
    private final Map<String, List<DeleteMessage>> deleteCollections;
    protected final AtomicBoolean running = new AtomicBoolean(true);
    protected final VisibilityMonitor visibilityMonitor;

    public DeletePublisher(final SqsClient sqsClient, final VisibilityMonitor visibilityMonitor)
    {
        this.sqsClient = sqsClient;
        this.visibilityMonitor = visibilityMonitor;
        this.deleteCollections = new HashMap<>();
    }

    @Override
    public void run()
    {
        while (running.get()) {
            send();
            try {
                // Serves to allow some degree of batching + saving CPU
                Thread.sleep(5000); // DDD long poll interval??
            } catch (final InterruptedException e) {
                LOG.error("A pause in task deletion was interrupted", e);
            }
        }
    }

    private void send()
    {
        for(final var entry : deleteCollections.entrySet()) {
            final var deleteMessages = entry.getValue();
            final var queueUrl = entry.getKey();
            synchronized (deleteMessages) {
                int deletedCount = 0;
                final var completedTasks = deleteMessages.stream()
                        .filter(dt -> dt.getSqsTaskInformation().processingComplete())
                        .collect(Collectors.toList());

                completedTasks.forEach(deleteMessages::remove);
                visibilityMonitor.unwatch(completedTasks.stream().map(DeleteMessage::getSqsTaskInformation).toList());

                final var batches = Iterables.partition(completedTasks, MAX_MESSAGE_BATCH_SIZE);
                final var failures = new ArrayList<DeletionError>();
                for(final var batch : batches) {
                    final var response = sendBatch(queueUrl, batch);
                    deletedCount += response.successes();
                    failures.addAll(response.errors());
                }
                LOG.info("Deleted {} message(s) from queue {}", deletedCount, queueUrl);

                if (!failures.isEmpty()) {
                    failures.forEach(error -> {
                        error.deleteMessage().incrementFailedDeleteCount();
                        LOG.info(error.toString());
                    });
                    // DDD should we be re-queueing failed deletes
                    //  whats the limit, whet then.
                    deleteMessages.addAll(failures.stream()
                            .map(DeletionError::deleteMessage)
                            .collect(Collectors.toList()));
                }
            }
        }
    }

    private DeleteBatchResponse sendBatch(
            final String queueUrl,
            final List<DeleteMessage> deleteMessages
    )
    {
        // DDD api calls here
        final Map<String, DeleteMessage> deleteMessageMap = new HashMap<>();
        final var entries = new ArrayList<DeleteMessageBatchRequestEntry>();
        int id = 1;
        for(final DeleteMessage message : deleteMessages) {
            final var idStr = String.valueOf(id++);
            deleteMessageMap.put(idStr, message);
            entries.add(DeleteMessageBatchRequestEntry.builder()
                    .id(idStr)
                    .receiptHandle(message.getSqsTaskInformation().getReceiptHandle())
                    .build());
        }
        final var request = DeleteMessageBatchRequest.builder()
                .entries(entries)
                .queueUrl(queueUrl)
                .build();

        final var response = sqsClient.deleteMessageBatch(request);
        final var succeeded = response.successful();
        succeeded.forEach(deleted-> LOG.debug("Deleted message {}", deleteMessageMap.get(deleted.id())));

        final var failed = response.failed();
        final var errors = failed.stream()
                .map(f -> new DeletionError(f.message(), deleteMessageMap.get(f.id())))
                .collect(Collectors.toList());
        return new DeleteBatchResponse(succeeded.size(), errors);
    }

    public void publish(final DeleteMessage deleteMessage)
    {
        final var taskInfo = deleteMessage.getSqsTaskInformation();
        final var queueInfo = taskInfo.getVisibilityTimeout().getQueueInfo();
        final var deleteTasks = deleteCollections.computeIfAbsent(
                queueInfo.url(),
                (q) -> Collections.synchronizedList(new ArrayList<>())
        );
        deleteTasks.add(deleteMessage); // Tasks are unique by receipt handle.
        LOG.debug("Awaiting processing complete for {}", taskInfo.getReceiptHandle());
    }

    public void shutdown()
    {
        running.set(false);
    }
}
