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

public class VisibilityMonitor implements Runnable
{
    private final SqsClient sqsClient;
    private final String queueUrl;
    private final int visibilityTimeout;
    private final int visibilityBoundary;
    private final int halfTheVisibilityTimeout;
    private final Set<VisibilityTimeout> timeoutSet;

    private static final int MAX_BATCH_SIZE = 10;
    private static final int SAFETY_BUFFER_SECONDS = 120;

    private static final Logger LOG = LoggerFactory.getLogger(VisibilityMonitor.class);

    public VisibilityMonitor(
            final SqsClient sqsClient,
            final String queueUrl,
            final int queueVisibilityTimeout
    )
    {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
        this.visibilityTimeout = queueVisibilityTimeout;
        timeoutSet = Collections.synchronizedSortedSet(new TreeSet<>());
        visibilityBoundary = queueVisibilityTimeout + SAFETY_BUFFER_SECONDS;
        halfTheVisibilityTimeout = (visibilityTimeout / 2) * 1000;
    }

    @Override
    public void run()
    {
        while (true) {
            try {
                final var expired = new HashSet<VisibilityTimeout>();
                final var toBeExtended = new HashSet<VisibilityTimeout>();
                final var newTimeouts = new HashSet<VisibilityTimeout>();

                synchronized (timeoutSet) {
                    for(final var to : timeoutSet) {
                        if (to.getBecomesVisible().isBefore(Instant.now())) {
                            expired.add(to);
                        } else if (to.getBecomesVisible().isBefore(Instant.now().plusSeconds(visibilityBoundary))) {
                            LOG.debug("Going to extend {}", to);
                            toBeExtended.add(to);
                        } else {
                            // Anything past here is safe on this iteration.
                            break;
                        }
                    }

                    // remove all expired and about to expire.
                    timeoutSet.removeAll(expired);
                    timeoutSet.removeAll(toBeExtended);

                    // Get all message receiptHandles to extend
                    // And extend the task info visibility
                    for(final var taskInfo : toBeExtended) {
                        final var visibility = taskInfo.getBecomesVisible().plusSeconds(visibilityTimeout);
                        final var newTimeout = new VisibilityTimeout(visibility, taskInfo.getReceiptHandle());
                        newTimeouts.add(newTimeout);
                    }

                    final var failures = extendTaskTimeouts(newTimeouts);
                    failures.forEach(msg -> LOG.debug(msg.toString()));

                    newTimeouts.removeAll(failures);

                    newTimeouts.forEach(to -> LOG.debug("Extended timout: {}", to));

                    timeoutSet.addAll(newTimeouts);
                }

                Thread.sleep(halfTheVisibilityTimeout);
            } catch (final InterruptedException e) {
                LOG.error("A pause in extending timeouts was interrupted", e);
            }
        }
    }

    private Set<VisibilityTimeout> extendTaskTimeouts(final HashSet<VisibilityTimeout> timeouts)
    {
        final var failures = new HashSet<VisibilityTimeout>();
        final var batches = Iterables.partition(timeouts, MAX_BATCH_SIZE);
        for(final var batch : batches) {
            failures.addAll(sendBatch(batch));
        }
        return failures;
    }

    private Set<VisibilityTimeout> sendBatch(final List<VisibilityTimeout> timeouts)
    {
        final Map<String, VisibilityTimeout> timeoutMap = new HashMap<>();
        final var entries = new ArrayList<ChangeMessageVisibilityBatchRequestEntry>();
        int id = 1;
        for(final VisibilityTimeout receiptHandle : timeouts) {
            var idStr = String.valueOf(id++);
            timeoutMap.put(idStr, receiptHandle);
            entries.add(ChangeMessageVisibilityBatchRequestEntry.builder()
                    .id(idStr)
                    .receiptHandle(receiptHandle.getReceiptHandle())
                    .visibilityTimeout(visibilityTimeout)
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
        var to = new VisibilityTimeout(taskInfo);
        LOG.debug("Watching {}", to);
        timeoutSet.add(to);
    }

    public void unwatch(final SQSTaskInformation taskInfo)
    {
        final var unwatchReceiptHandle = taskInfo.getReceiptHandle();
        synchronized (timeoutSet) {
            var unwatchOpt = timeoutSet.stream()
                    .filter(to->to.getReceiptHandle().equals(unwatchReceiptHandle))
                    .findFirst();
            if (unwatchOpt.isPresent()) {
                var removed = timeoutSet.remove(unwatchOpt.get());
                if (removed) {
                    LOG.debug("Unwatched receipt handle {}", unwatchReceiptHandle);
                } else {
                    LOG.error("Failed to unwatch receipt handle {}", unwatchReceiptHandle);
                }
            }
        }
    }
}
