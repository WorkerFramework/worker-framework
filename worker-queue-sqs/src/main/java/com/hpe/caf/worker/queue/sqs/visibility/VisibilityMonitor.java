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
package com.hpe.caf.worker.queue.sqs.visibility;

import com.google.common.collect.Iterables;
import com.hpe.caf.worker.queue.sqs.QueueInfo;
import com.hpe.caf.worker.queue.sqs.SQSTaskInformation;
import com.hpe.caf.worker.queue.sqs.config.SQSWorkerQueueConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static com.hpe.caf.worker.queue.sqs.util.SQSUtil.MAX_MESSAGE_BATCH_SIZE;
import static com.hpe.caf.worker.queue.sqs.util.SQSUtil.getExpiry;

/**
 * This class monitors visibility timeouts for queues and batches up extension requests.
 */
public class VisibilityMonitor implements Runnable
{
    private final SqsClient sqsClient;
    private final int queueVisibilityTimeout;
    private final int monitoringInterval;
    private final Map<String, List<VisibilityTimeout>> timeoutCollections;
    protected final AtomicBoolean running = new AtomicBoolean(true);

    private static final Logger LOG = LoggerFactory.getLogger(VisibilityMonitor.class);

    public VisibilityMonitor(
            final SqsClient sqsClient,
            final int queueVisibilityTimeout
    )
    {
        this.sqsClient = sqsClient;
        this.queueVisibilityTimeout = queueVisibilityTimeout;
        timeoutCollections = new HashMap<>();
        monitoringInterval = (this.queueVisibilityTimeout / 2) * 1000;
    }

    @Override
    public void run()
    {
        while (running.get()) {
            try {
                for(final var entry : timeoutCollections.entrySet()) {
                    final var visibilityTimeouts = entry.getValue();
                    final var queueInfo = entry.getKey();

                    final var now = Instant.now().getEpochSecond();
                    final var boundary = now + (queueVisibilityTimeout * 2L);

                    final var expiredTimeouts = new ArrayList<VisibilityTimeout>();
                    final var toBeExtendedTimeouts = new ArrayList<VisibilityTimeout>();
                    final var changeVisibilityErrors = new ArrayList<ChangeVisibilityError>();

                    synchronized (visibilityTimeouts) {

                        Collections.sort(visibilityTimeouts);
                        for(final var vto : visibilityTimeouts) {
                            if (vto.getBecomesVisibleEpochSecond() <= now) {
                                expiredTimeouts.add(vto);
                            } else if (vto.getBecomesVisibleEpochSecond() < boundary) {
                                toBeExtendedTimeouts.add(vto);
                            } else {
                                break;
                            }
                        }

                        // remove all expired.
                        visibilityTimeouts.removeAll(expiredTimeouts);

                        // Get all message receiptHandles to extend
                        // And extend the task info visibility
                        for(final var visibilityTimeout : toBeExtendedTimeouts) {
                            final var visibility = Instant.now().getEpochSecond() + (queueVisibilityTimeout * 2L);
                            visibilityTimeout.setBecomesVisibleEpochSecond(visibility);
                        }

                        changeVisibilityErrors.addAll(extendTaskTimeouts(queueInfo, toBeExtendedTimeouts));
                        final var errors = changeVisibilityErrors.stream()
                                .map(ChangeVisibilityError::visibilityTimeout)
                                .toList();
                        visibilityTimeouts.removeAll(errors);
                    }

                    // Not yet observed any expired timeouts, and should not, but we want to see them.
                    expiredTimeouts.forEach(to -> LOG.info("Timeout expired at:{} for:{}",
                            getExpiry(to), to.getReceiptHandle()));

                    // Not yet observed any failures, and should not, but we want to see them.
                    changeVisibilityErrors.forEach(f -> LOG.error(f.toString()));

                    toBeExtendedTimeouts.forEach(to -> LOG.debug("Extended timeout to:{} for:{}",
                            getExpiry(to), to.getReceiptHandle()));
                }

                Thread.sleep(monitoringInterval);
            } catch (final InterruptedException e) {
                LOG.error("A pause in extending timeouts was interrupted", e);
            }
        }
    }

    private List<ChangeVisibilityError> extendTaskTimeouts(
            final String queueUrl,
            final List<VisibilityTimeout> timeouts
    )
    {
        final var failures = new ArrayList<ChangeVisibilityError>();
        final var batches = Iterables.partition(timeouts, MAX_MESSAGE_BATCH_SIZE);
        for(final var batch : batches) {
            final var failed = sendBatch(queueUrl, batch);
            failures.addAll(failed);
        }
        return failures;
    }

    private List<ChangeVisibilityError> sendBatch(
            final String queueUrl,
            final List<VisibilityTimeout> timeouts
    )
    {
        final Map<String, VisibilityTimeout> timeoutMap = new HashMap<>();
        final var entries = new ArrayList<ChangeMessageVisibilityBatchRequestEntry>();
        int id = 1;
        for(final VisibilityTimeout to : timeouts) {
            final var idStr = String.valueOf(id++);
            timeoutMap.put(idStr, to);
            final long sqsTimeout = to.getBecomesVisibleEpochSecond() - Instant.now().getEpochSecond();
            entries.add(ChangeMessageVisibilityBatchRequestEntry.builder()
                    .id(idStr)
                    .receiptHandle(to.getReceiptHandle())
                    .visibilityTimeout((int) sqsTimeout)
                    .build());
        }
        final var request = ChangeMessageVisibilityBatchRequest.builder()
                .entries(entries)
                .queueUrl(queueUrl)
                .build();

        return sqsClient.changeMessageVisibilityBatch(request)
                .failed()
                .stream()
                .map(f -> new ChangeVisibilityError(f.message(), timeoutMap.get(f.id())))
                .collect(Collectors.toList());
    }

    public void watch(final SQSTaskInformation taskInfo)
    {
        final var visibilityTimeout = taskInfo.getVisibilityTimeout();
        final var queueInfo = visibilityTimeout.getQueueInfo();
        final var visibilityTimeouts = timeoutCollections.computeIfAbsent(
                queueInfo.url(),
                (q) -> Collections.synchronizedList(new ArrayList<>())
        );
        visibilityTimeouts.add(visibilityTimeout);
        LOG.debug("Watching {}", taskInfo.getReceiptHandle());
        logInflightTasks(taskInfo, visibilityTimeouts);
    }

    public void unwatch(final List<SQSTaskInformation> taskInfoList)
    {
        taskInfoList.forEach(this::unwatch);
    }

    public void unwatch(final SQSTaskInformation taskInfo)
    {
        final var visibilityTimeouts = timeoutCollections.get(taskInfo.getQueueInfo().url());
        if (visibilityTimeouts != null) {
            synchronized (visibilityTimeouts) {
                final var removed = visibilityTimeouts.remove(taskInfo.getVisibilityTimeout());
                if (removed) {
                    LOG.debug("Unwatched {}", taskInfo.getReceiptHandle());
                    logInflightTasks(taskInfo, visibilityTimeouts);
                }
            }
        }
    }

    public boolean hasInflightCapacity(
            final QueueInfo queueInfo,
            final SQSWorkerQueueConfiguration queueCfg,
            final int batchSize
    )
    {
        final var inflightMessages = inflightMessages(queueInfo).size();
        final var capacity = (queueCfg.getMaxInflightMessages() - inflightMessages);
        final boolean hasCapacity =  capacity >= batchSize;
        if (!hasCapacity) {
            LOG.debug("queue {} has no capacity, inflight message count: {}",
                    queueInfo.name(),
                    inflightMessages);
        }
        return hasCapacity;
    }

    private List<VisibilityTimeout> inflightMessages(final QueueInfo queueInfo)
    {
        return timeoutCollections.getOrDefault(queueInfo.url(), new ArrayList<>());
    }

    private static void logInflightTasks(final SQSTaskInformation taskInfo, final List<VisibilityTimeout> timeouts)
    {
        LOG.debug("queue {} now has {} tasks inflight",
                taskInfo.getQueueInfo().name(),
                timeouts.size());
    }

    public void shutdown()
    {
        running.set(false);
    }
}
