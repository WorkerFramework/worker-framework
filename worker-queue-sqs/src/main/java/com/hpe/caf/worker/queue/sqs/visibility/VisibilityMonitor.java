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
import com.hpe.caf.worker.queue.sqs.SQSTaskInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * This class monitors visibility timeouts for queues.
 */
public class VisibilityMonitor implements Runnable
{
    private final SqsClient sqsClient;
    private final int queueVisibilityTimeout;
    private final int timeoutWindowLimit;
    private final int monitoringInterval;
    private final Map<String, Set<VisibilityTimeout>> timeoutSets;

    private static final int MAX_BATCH_SIZE = 10;
    private static final int SAFETY_BUFFER_SECONDS = 120;

    private static final Logger LOG = LoggerFactory.getLogger(VisibilityMonitor.class);

    public VisibilityMonitor(
            final SqsClient sqsClient,
            final int queueVisibilityTimeout
    )
    {
        this.sqsClient = sqsClient;
        this.queueVisibilityTimeout = queueVisibilityTimeout;
        timeoutSets = new HashMap<>();
        timeoutWindowLimit = queueVisibilityTimeout + SAFETY_BUFFER_SECONDS;
        monitoringInterval = (this.queueVisibilityTimeout / 2) * 1000;
    }

    @Override
    public void run()
    {
        while (true) {
            try {
                final var expiredTimeouts = new HashSet<VisibilityTimeout>();
                final var toBeExtendedTimeouts = new HashSet<VisibilityTimeout>();
                final var newTimeouts = new HashSet<VisibilityTimeout>();
                for (final var entry : timeoutSets.entrySet()) {
                    final var timeoutSet = entry.getValue();
                    final var queueInfo = entry.getKey();
                    synchronized (timeoutSet) {
                        var now = Instant.now().getEpochSecond();
                        var boundary = now + timeoutWindowLimit;
                        for(final var vto : timeoutSet) {
                            if (vto.becomesVisibleEpochSecond() < now) {
                                expiredTimeouts.add(vto);
                            } else if (vto.becomesVisibleEpochSecond() < boundary) {
                                LOG.debug("Going to extend {}", vto);
                                toBeExtendedTimeouts.add(vto);
                            } else {
                                // Anything past here is safe on this iteration.
                                break;
                            }
                        }

                        // remove all expired and about to expire.
                        timeoutSet.removeAll(expiredTimeouts);
                        timeoutSet.removeAll(toBeExtendedTimeouts);

                        // Get all message receiptHandles to extend
                        // And extend the task info visibility
                        for(final var visibilityTimeout : toBeExtendedTimeouts) {
                            final var visibility = visibilityTimeout.becomesVisibleEpochSecond() + queueVisibilityTimeout;
                            final var newTimeout = new VisibilityTimeout(
                                    visibilityTimeout.queueInfo(),
                                    visibility,
                                    visibilityTimeout.receiptHandle()
                            );
                            newTimeouts.add(newTimeout);
                        }

                        final var failures = extendTaskTimeouts(queueInfo, newTimeouts);
                        failures.forEach(msg -> LOG.debug(msg.toString()));

                        newTimeouts.removeAll(failures);

                        newTimeouts.forEach(to -> LOG.debug("Extended timout: {}", to));

                        timeoutSet.addAll(newTimeouts);
                    }
                }

                Thread.sleep(monitoringInterval);
            } catch (final InterruptedException e) {
                LOG.error("A pause in extending timeouts was interrupted", e);
            }
        }
    }

    private Set<VisibilityTimeout> extendTaskTimeouts(
            final String queueUrl,
            final HashSet<VisibilityTimeout> timeouts
    )
    {
        final var failures = new HashSet<VisibilityTimeout>();
        final var batches = Iterables.partition(timeouts, MAX_BATCH_SIZE);
        for(final var batch : batches) {
            failures.addAll(sendBatch(queueUrl, batch));
        }
        return failures;
    }

    private Set<VisibilityTimeout> sendBatch(
            final String queueUrl,
            final List<VisibilityTimeout> timeouts
    )
    {
        final Map<String, VisibilityTimeout> timeoutMap = new HashMap<>();
        final var entries = new ArrayList<ChangeMessageVisibilityBatchRequestEntry>();
        int id = 1;
        for(final VisibilityTimeout receiptHandle : timeouts) {
            var idStr = String.valueOf(id++);
            timeoutMap.put(idStr, receiptHandle);
            entries.add(ChangeMessageVisibilityBatchRequestEntry.builder()
                    .id(idStr)
                    .receiptHandle(receiptHandle.receiptHandle())
                    .visibilityTimeout(queueVisibilityTimeout)
                    .build());
        }
        final var request = ChangeMessageVisibilityBatchRequest.builder()
                .entries(entries)
                .queueUrl(queueUrl)
                .build();

        return sqsClient.changeMessageVisibilityBatch(request)
                .failed()
                .stream()
                .map(f -> timeoutMap.get(f.id()))
                .collect(Collectors.toSet());
    }

    public void watch(final SQSTaskInformation taskInfo)
    {
        LOG.debug("Watching {}", taskInfo);
        final var visibilityTimeout = taskInfo.getVisibilityTimeout();
        final var queueInfo = visibilityTimeout.queueInfo();
        final var timeoutSet = timeoutSets.computeIfAbsent(
            queueInfo.url(),
            (q) -> Collections.synchronizedSortedSet(new TreeSet<>())
        );
        timeoutSet.add(visibilityTimeout);
    }

    public void unwatch(final SQSTaskInformation taskInfo)
    {
        final var unwatchReceiptHandle = taskInfo.getReceiptHandle();
        final var timeoutSet = timeoutSets.get(taskInfo.getQueueInfo().url());
        if (timeoutSet != null) {
            synchronized (timeoutSet) {
                var unwatchOpt = timeoutSet.stream()
                        .filter(to -> to.receiptHandle().equals(unwatchReceiptHandle))
                        .findFirst();
                if (unwatchOpt.isPresent()) {
                    var removed = timeoutSet.remove(unwatchOpt.get());
                    if (removed) {
                        LOG.debug("Unwatched {}", taskInfo);
                    } else {
                        LOG.error("Failed to unwatch {}", taskInfo);
                    }
                }
            }
        }
    }
}
