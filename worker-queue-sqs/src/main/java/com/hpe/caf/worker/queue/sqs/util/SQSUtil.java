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
package com.hpe.caf.worker.queue.sqs.util;

import com.hpe.caf.configs.SQSConfiguration;
import com.hpe.caf.worker.queue.sqs.QueueInfo;
import com.hpe.caf.worker.queue.sqs.config.SQSWorkerQueueConfiguration;
import com.hpe.caf.worker.queue.sqs.visibility.VisibilityTimeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.CreateQueueRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueAttributesRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.QueueAttributeName;
import software.amazon.awssdk.services.sqs.model.QueueNameExistsException;
import software.amazon.awssdk.services.sqs.model.SetQueueAttributesRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SQSUtil
{
    public static final String SQS_HEADER_CAF_WORKER_REJECTED = "x-caf-worker-rejected";
    public static final String REJECTED_REASON_TASKMESSAGE = "TASKMESSAGE_INVALID";
    public static final String DEAD_LETTER_QUEUE_SUFFIX = "-dlq";
    public static final String ALL_ATTRIBUTES = "All";
    public static final String SOURCE_QUEUE = "SourceQueue";
    public static final int MAX_MESSAGE_BATCH_SIZE = 10;

    private static final Logger LOG = LoggerFactory.getLogger(SQSUtil.class);

    public static SqsClient getSqsClient(final SQSConfiguration sqsConfiguration) throws URISyntaxException
    {
        return SqsClient.builder()
                .endpointOverride(new URI(sqsConfiguration.getURIString()))
                .region(Region.of(sqsConfiguration.getSqsRegion()))
                .credentialsProvider(() -> getAWSCredentials(sqsConfiguration))
                .build();
    }

    public static AwsCredentials getAWSCredentials(final SQSConfiguration sqsConfiguration)
    {
        return new AwsCredentials()
        {
            @Override
            public String accessKeyId()
            {
                return sqsConfiguration.getSqsAccessKey();
            }

            @Override
            public String secretAccessKey()
            {
                return sqsConfiguration.getSqsSecretAccessKey();
            }
        };
    }

    /**
     * This method creates a DeadLetterQueue and associates it with a source Queue.
     */
    public static QueueInfo createDeadLetterQueue(
            final SqsClient sqsClient,
            final QueueInfo sourceQueue,
            final SQSWorkerQueueConfiguration queueCfg)
    {
        final var queueName = sourceQueue.name() + DEAD_LETTER_QUEUE_SUFFIX;
        final var response = createQueue(sqsClient, queueName, queueCfg);

        addRedrivePolicy(sqsClient, sourceQueue.url(), response.arn(), queueCfg);

        return response;
    }

    public static QueueInfo createQueue(
            final SqsClient sqsClient,
            final String queueName,
            final SQSWorkerQueueConfiguration queueCfg)
    {
        try {
            final var createQueueRequest = CreateQueueRequest.builder()
                    .queueName(queueName)
                    .attributes(getQueueAttributes(queueCfg))
                    .build();

            final var response = sqsClient.createQueue(createQueueRequest);
            final var url = response.queueUrl();
            final var arn = getQueueArn(sqsClient, url);

            return new QueueInfo(queueName, url, arn);
        } catch (final QueueNameExistsException e) {
            LOG.info("Queue already exists {} {}", queueName, e.getMessage());
            return getQueueInfo(sqsClient, queueName);
        }
    }

    private static void addRedrivePolicy(
            final SqsClient sqsClient,
            final String inputQueueUrl,
            final String deadLetterQueueArn,
            final SQSWorkerQueueConfiguration queueCfg)
    {
        final var sourceAttributes = new HashMap<QueueAttributeName, String>();
        sourceAttributes.put(
                QueueAttributeName.REDRIVE_POLICY,
                String.format("{\"maxReceiveCount\":\"%d\", \"deadLetterTargetArn\":\"%s\"}",
                        queueCfg.getMaxDeliveries(), deadLetterQueueArn)
        );

        var queueAttributesRequest = SetQueueAttributesRequest.builder()
                .queueUrl(inputQueueUrl)
                .attributes(sourceAttributes)
                .build();
        sqsClient.setQueueAttributes(queueAttributesRequest);
    }

    public static Map<QueueAttributeName, String> getQueueAttributes(final SQSWorkerQueueConfiguration queueCfg)
    {
        final var attributes = new HashMap<QueueAttributeName, String>();
        attributes.put(
                QueueAttributeName.VISIBILITY_TIMEOUT,
                String.valueOf(queueCfg.getVisibilityTimeout())
        );

        attributes.put(
                QueueAttributeName.MESSAGE_RETENTION_PERIOD,
                String.valueOf(queueCfg.getMessageRetentionPeriod())
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

    public static String getQueueArn(final SqsClient sqsClient, final String queueUrl)
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

    public static Date getExpiry(final VisibilityTimeout visibilityTimeout)
    {
        return new Date(visibilityTimeout.getBecomesVisibleEpochSecond() * 1000);
    }
}
