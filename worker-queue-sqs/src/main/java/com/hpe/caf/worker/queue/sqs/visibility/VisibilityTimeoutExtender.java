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
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class VisibilityTimeoutExtender implements Runnable
{
    private final SqsClient sqsClient;
    private final String queueUrl;
    private final int MAX_BATCH_SIZE = 10;
    private final int defaultTimeout;
    public final Set<SQSTaskInformation> timeoutSortedSet;

    private static final Logger LOG = LoggerFactory.getLogger(VisibilityTimeoutExtender.class);

    public VisibilityTimeoutExtender(
            final SqsClient sqsClient,
            final String queueUrl,
            final int defaultTimeout,
            final Set<SQSTaskInformation> timeoutSortedSet
    )
    {
        this.sqsClient = sqsClient;
        this.queueUrl = queueUrl;
        this.defaultTimeout = defaultTimeout;
        this.timeoutSortedSet = timeoutSortedSet;
    }

    @Override
    public void run()
    {
        while (true) {
            try {
                final var expired = new ArrayList<SQSTaskInformation>();
                final var toBeExtended = new ArrayList<SQSTaskInformation>();

                for (SQSTaskInformation ti : timeoutSortedSet) {
                    if (ti.getBecomesVisible().isBefore(Instant.now())) {
                        expired.add(ti);
                    } else if (ti.getBecomesVisible().isBefore(Instant.now().plusSeconds(defaultTimeout))) {
                        toBeExtended.add(ti);
                    } else {
                        // Anything past here is safe this time.
                        break;
                    }
                }

                // remove all expired and about to expire.
                timeoutSortedSet.removeAll(expired);
                timeoutSortedSet.removeAll(toBeExtended);

                // Now extend any that are half way to becoming visible
                if (!toBeExtended.isEmpty()) {
                    extendTaskTimeout(toBeExtended);
                    for (var taskInfo : toBeExtended) {
                        taskInfo.setBecomesVisible(taskInfo.getBecomesVisible().plusSeconds(defaultTimeout));
                    }
                }

                // Put back to the sorted set
                timeoutSortedSet.addAll(toBeExtended);

                Thread.sleep((defaultTimeout / 2) * 1000);
            } catch (final InterruptedException e) {
                LOG.error("Extending timeouts interrupted", e);
            }
        }
    }

    private List<BatchResultErrorEntry> extendTaskTimeout(
            final Collection<SQSTaskInformation> taskInfo
    )
    {
        final var failures = new ArrayList<BatchResultErrorEntry>();
        final var batches = Iterables.partition(taskInfo, MAX_BATCH_SIZE);
        for (final var batch : batches) {
            failures.addAll(sendBatch(batch));
        }
        failures.forEach(msg -> {
            LOG.error(msg.toString());
        });
        return failures;
    }

    private List<BatchResultErrorEntry> sendBatch(
            final List<SQSTaskInformation> taskInfo
    )
    {
        final var entries = new ArrayList<ChangeMessageVisibilityBatchRequestEntry>();
        int id = 1;
        for (final SQSTaskInformation ti : taskInfo) {
            entries.add(ChangeMessageVisibilityBatchRequestEntry.builder()
                    .id(String.valueOf(id++))
                    .receiptHandle(ti.getReceiptHandle())
                    .visibilityTimeout(defaultTimeout)
                    .build());
        }
        final var request = ChangeMessageVisibilityBatchRequest.builder()
                .entries(entries)
                .queueUrl(queueUrl)
                .build();
        return sqsClient.changeMessageVisibilityBatch(request).failed();
    }

    private static Predicate<SQSTaskInformation> timeoutPredicate(
            final int visibilityTimeout
    )
    {
        return (ti) -> {
            return ti.getBecomesVisible().isBefore(Instant.now().plusSeconds(visibilityTimeout));
        };
    }

    private static Predicate<SQSTaskInformation> expiredPredicate()
    {
        return (ti) -> {
            return ti.getBecomesVisible().isBefore(Instant.now());
        };
    }
}
