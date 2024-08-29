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
package com.hpe.caf.worker.queue.sqs;

import com.hpe.caf.worker.queue.sqs.config.SQSWorkerQueueConfiguration;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;

import java.util.HashMap;
import java.util.Map;

public class SQSUtil
{
    public static final String SQS_HEADER_CAF_WORKER_REJECTED = "x-caf-worker-rejected";
    public static final String REJECTED_REASON_TASKMESSAGE = "TASKMESSAGE_INVALID";
    public static final String DEAD_LETTER_QUEUE_SUFFIX = "-dlq";
    public static final String ALL_ATTRIBUTES = "All";
    public static final String SOURCE_QUEUE = "SourceQueue";

    static Map<QueueAttributeName, String> getInputQueueAttributes(final SQSWorkerQueueConfiguration sqsQueueCfg)
    {
        final var attributes = new HashMap<QueueAttributeName, String>();
        attributes.put(
                QueueAttributeName.VISIBILITY_TIMEOUT,
                String.valueOf(sqsQueueCfg.getVisibilityTimeout())
        );

        attributes.put(
                QueueAttributeName.MESSAGE_RETENTION_PERIOD,
                String.valueOf(sqsQueueCfg.getMessageRetentionPeriod())
        );

        return attributes;
    }

    static Map<QueueAttributeName, String> getDeadLetterQueueAttributes(final SQSWorkerQueueConfiguration sqsQueueCfg)
    {
        final var attributes = new HashMap<QueueAttributeName, String>();

        attributes.put(
                QueueAttributeName.VISIBILITY_TIMEOUT,
                String.valueOf(sqsQueueCfg.getDlqVisibilityTimeout())
        );

        attributes.put(
                QueueAttributeName.MESSAGE_RETENTION_PERIOD,
                String.valueOf(sqsQueueCfg.getDlqMessageRetentionPeriod())
        );

        return attributes;
    }

    public static String getQueueUrl(final SqsClient sqsClient, final String queueName)
    {
        final var getQueueUrlRequest = GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build();
        return sqsClient.getQueueUrl(getQueueUrlRequest).queueUrl();
    }

    static String getQueueArn(final SqsClient sqsClient, final String queueUrl)
    {
        final var attributesResponse = sqsClient.getQueueAttributes(
                GetQueueAttributesRequest.builder()
                        .queueUrl(queueUrl)
                        .attributeNames(QueueAttributeName.QUEUE_ARN)
                        .build());
        return attributesResponse.attributes().get(QueueAttributeName.QUEUE_ARN);
    }

    public static QueueInfo getQueueInfo(
            final SqsClient sqsClient,
            final String queueName
    )
    {
        var url = getQueueUrl(sqsClient, queueName);
        var arn = getQueueArn(sqsClient, url);
        return new QueueInfo(queueName, url, arn);
    }
}
