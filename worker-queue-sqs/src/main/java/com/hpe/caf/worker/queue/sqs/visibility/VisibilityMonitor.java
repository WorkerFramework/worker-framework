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
import software.amazon.awssdk.services.sqs.model.BatchResultErrorEntry;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class VisibilityMonitor implements Runnable
{
    private final SqsClient sqsClient;
    private final String queueUrl;
    private final int visibilityTimeout;
    private final int visibilityBoundary;
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
        this.timeoutSet = Collections.synchronizedSortedSet(new TreeSet<>());
        visibilityBoundary = queueVisibilityTimeout + SAFETY_BUFFER_SECONDS;
    }

    @Override
    public void run()
    {
        while (true) {
            try {
                final var expired = new ArrayList<VisibilityTimeout>();
                final var toBeExtended = new ArrayList<VisibilityTimeout>();
                final var newTimeouts = new ArrayList<VisibilityTimeout>();
                final var receiptHandlesToExtend = new ArrayList<String>();

                synchronized (timeoutSet) {
                    final var iter = timeoutSet.iterator(); // Must be in the synchronized block
                    while (iter.hasNext()) {
                        final VisibilityTimeout ti = iter.next();
                        if (ti.getBecomesVisible().isBefore(Instant.now())) {
                            expired.add(ti);
                        } else if (ti.getBecomesVisible().isBefore(Instant.now().plusSeconds(visibilityBoundary))) {
                            toBeExtended.add(ti);
                        } else {
                            // Anything past here is safe this time.
                            break;
                        }
                    }

                    // remove all expired and about to expire.
                    timeoutSet.removeAll(expired);
                    timeoutSet.removeAll(toBeExtended);

                    // Get all message receiptHandles to extend
                    // And extend the task info visibility
                    if (!toBeExtended.isEmpty()) {
                        for(var taskInfo : toBeExtended) {
                            receiptHandlesToExtend.add(taskInfo.getReceiptHandle());
                            var visibility = taskInfo.getBecomesVisible().plusSeconds(visibilityTimeout);
                            newTimeouts.add(new VisibilityTimeout(visibility, taskInfo.getReceiptHandle()));
                        }
                    }
                    // Put back to the sorted set
                    timeoutSet.addAll(toBeExtended);
                }

                extendTaskTimeouts(receiptHandlesToExtend);

                Thread.sleep((visibilityTimeout / 2) * 1000);
            } catch (final InterruptedException e) {
                LOG.error("A pause in extending timeouts was interrupted", e);
            }
        }
    }

    private List<BatchResultErrorEntry> extendTaskTimeouts(
            final Collection<String> receiptHandles
    )
    {
        final var failures = new ArrayList<BatchResultErrorEntry>();
        final var batches = Iterables.partition(receiptHandles, MAX_BATCH_SIZE);
        for(final var batch : batches) {
            failures.addAll(sendBatch(batch));
        }
        failures.forEach(msg -> {
            LOG.error(msg.toString());
        });
        return failures;
    }

    private List<BatchResultErrorEntry> sendBatch(
            final List<String> receiptHandles
    )
    {
        final var entries = new ArrayList<ChangeMessageVisibilityBatchRequestEntry>();
        int id = 1;
        for(final String receiptHandle : receiptHandles) {
            entries.add(ChangeMessageVisibilityBatchRequestEntry.builder()
                    .id(String.valueOf(id++))
                    .receiptHandle(receiptHandle)
                    .visibilityTimeout(visibilityTimeout)
                    .build());
        }
        final var request = ChangeMessageVisibilityBatchRequest.builder()
                .entries(entries)
                .queueUrl(queueUrl)
                .build();
        return sqsClient.changeMessageVisibilityBatch(request).failed();
    }

    public void watch(final SQSTaskInformation taskInfo)
    {
        timeoutSet.add(new VisibilityTimeout(taskInfo));
    }

    public void unwatch(final SQSTaskInformation taskInfo)
    {
        timeoutSet.remove(new VisibilityTimeout(taskInfo));
    }
}
