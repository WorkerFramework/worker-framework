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
package com.hpe.caf.worker.queue.sqs.deletion;

import com.google.common.collect.Iterables;
import com.hpe.caf.worker.queue.sqs.SQSTaskInformation;
import com.hpe.caf.worker.queue.sqs.visibility.VisibilityMonitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.hpe.caf.worker.queue.sqs.util.SQSUtil.MAX_MESSAGE_BATCH_SIZE;

public class DeletePublisher implements Runnable
{
    private static final Logger LOG = LoggerFactory.getLogger(DeletePublisher.class);

    private final SqsClient sqsClient;
    private final Map<String, Set<SQSTaskInformation>> deleteCollections;
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
            processTasks();
            try {
                // Serves to allow some degree of batching + saving CPU
                Thread.sleep(5000);
            } catch (final InterruptedException e) {
                LOG.error("A pause in task deletion was interrupted", e);
            }
        }
    }

    private void processTasks()
    {
        for(final var entry : deleteCollections.entrySet()) {
            final var tasks = entry.getValue();
            synchronized (tasks) {
                final var completedTasks = tasks.stream()
                        .filter(SQSTaskInformation::processingComplete)
                        .collect(Collectors.toList());
                completedTasks.forEach(tasks::remove);
                visibilityMonitor.unwatch(completedTasks);
                final var errors = deleteTasks(entry.getKey(), completedTasks);

                if (!errors.isEmpty()) {
                    errors.forEach(error -> LOG.info(error.toString()));
                }
            }
        }
    }

    private List<DeletionError> deleteTasks(
            final String queueUrl,
            final List<SQSTaskInformation> completedTasks
    )
    {
        final var failures = new ArrayList<DeletionError>();
        final var batches = Iterables.partition(completedTasks, MAX_MESSAGE_BATCH_SIZE);
        for(final var batch : batches) {
            final var failed = sendBatch(queueUrl, batch);
            failures.addAll(failed);
        }

        return failures;
    }

    private List<DeletionError> sendBatch(
            final String queueUrl,
            final List<SQSTaskInformation> tasks
    )
    {
        final Map<String, SQSTaskInformation> taskMap = new HashMap<>();
        final var entries = new ArrayList<DeleteMessageBatchRequestEntry>();
        int id = 1;
        for(final SQSTaskInformation task : tasks) {
            final var idStr = String.valueOf(id++);
            taskMap.put(idStr, task);
            entries.add(DeleteMessageBatchRequestEntry.builder()
                    .id(idStr)
                    .receiptHandle(task.getReceiptHandle())
                    .build());
        }
        final var request = DeleteMessageBatchRequest.builder()
                .entries(entries)
                .queueUrl(queueUrl)
                .build();

        final var response = sqsClient.deleteMessageBatch(request);

        response.successful().forEach(deleted-> LOG.debug("Deleted message {}", taskMap.get(deleted.id())));

        return response
                .failed()
                .stream()
                .map(f -> new DeletionError(f.message(), taskMap.get(f.id())))
                .collect(Collectors.toList());
    }

    public void watch(final SQSTaskInformation taskInfo)
    {
        final var queueInfo = taskInfo.getVisibilityTimeout().getQueueInfo();
        final var deleteTasks = deleteCollections.computeIfAbsent(
                queueInfo.url(),
                (q) -> Collections.synchronizedSet(new HashSet<>())
        );
        deleteTasks.add(taskInfo); // Tasks are unique by receipt handle.
        LOG.debug("Watching {}", taskInfo.getReceiptHandle());
    }
}
