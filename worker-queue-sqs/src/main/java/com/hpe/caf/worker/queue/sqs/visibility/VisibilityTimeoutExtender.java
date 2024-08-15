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
import com.hpe.caf.worker.queue.sqs.config.SQSWorkerQueueConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.BatchResultErrorEntry;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequest;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityBatchResponse;

import java.util.ArrayList;
import java.util.List;

public class VisibilityTimeoutExtender
{
    private final SqsClient sqsClient;
    private final SQSWorkerQueueConfiguration sqsQueueConfiguration;
    private final int MAX_BATCH_SIZE = 10;

    private static final Logger LOG = LoggerFactory.getLogger(VisibilityTimeoutExtender.class);

    public VisibilityTimeoutExtender(
            final SqsClient sqsClient,
            final SQSWorkerQueueConfiguration sqsQueueConfiguration
    )
    {
        this.sqsClient = sqsClient;
        this.sqsQueueConfiguration = sqsQueueConfiguration;
    }

    /**
     * This method extends the visibility timeout by double the pre-configured value.
     * @param queueUrl
     * @param taskInfo
     * @return
     */
    public List<BatchResultErrorEntry> extendTaskTimeout(
            final String queueUrl,
            final SQSTaskInformation... taskInfo
    )
    {
        return extendTaskTimeout(queueUrl, sqsQueueConfiguration.getVisibilityTimeout() * 2, taskInfo);
    }

    // DDD some more thought on how this would be calculated/detected as required.
    // DDD What do we get returned for errors
    // DDD any exceptions to handle
    // DDD tests required
    //  some have timeout already expired (receipt handle invalid)
    //  some for different queue
    public List<BatchResultErrorEntry> extendTaskTimeout(
            final String queueUrl,
            final int timeout,
            final SQSTaskInformation... taskInfo
    )
    {
        final var failures = new ArrayList<BatchResultErrorEntry>();
        final var taskInfos = Iterables.partition(List.of(taskInfo), MAX_BATCH_SIZE);
        for (final var taskInfoList : taskInfos) {
            var response = sendBatch(queueUrl, timeout, taskInfoList);
            failures.addAll(response.failed());
        }
        failures.forEach(msg->{
            LOG.error(msg.toString());
        });
        return failures;
    }

    private ChangeMessageVisibilityBatchResponse sendBatch(
            final String queueUrl,
            final int timeout,
            final List<SQSTaskInformation> taskInfo
    )
    {
        final var entries = new ArrayList<ChangeMessageVisibilityBatchRequestEntry>();
        for (final SQSTaskInformation ti : taskInfo) {
            entries.add(ChangeMessageVisibilityBatchRequestEntry.builder()
                    .id(ti.getInboundMessageId())
                    .receiptHandle(ti.getReceiptHandle())
                    .visibilityTimeout(timeout)
                    .build());
        }
        final var request = ChangeMessageVisibilityBatchRequest.builder()
                .entries(entries)
                .queueUrl(queueUrl)
                .build();
        return sqsClient.changeMessageVisibilityBatch(request);
    }
}
