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

import com.hpe.caf.api.worker.QueueException;
import com.hpe.caf.api.worker.TaskCallback;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;
import software.amazon.awssdk.services.sqs.paginators.ListQueuesIterable;

import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SQSWorkerQueue
{
    public SqsClient getSqsClient() {
        return sqsClient;
    }

    private SqsClient sqsClient;
    private Set<String> declaredQueues;

    private static final Logger LOG = LoggerFactory.getLogger(SQSWorkerQueue.class);

    public void start(final TaskCallback callback) throws QueueException {
        try {
            declaredQueues = new HashSet<>();
            sqsClient = SqsClient.builder()
                    .endpointOverride(new URI("http://localhost:19324"))
                    .region(Region.US_EAST_1)
                    .credentialsProvider(() -> new AwsCredentials() {
                        @Override
                        public String accessKeyId() {
                            return "x";
                        }

                        @Override
                        public String secretAccessKey() {
                            return "x";
                        }
                    })
                    .build();
            listQueues();
        } catch (final Exception e) {
            throw new QueueException("Failed to establish queues", e);
        }
    }

    public String createQueue(final String queueName) {
        final HashMap<QueueAttributeName, String> attributes = new HashMap<>();

        final CreateQueueRequest createRequest = CreateQueueRequest.builder()
                .queueName(queueName)
                .attributes(attributes)
                .build();

        sqsClient.createQueue(createRequest);
        final CreateQueueRequest request = CreateQueueRequest.builder()
                .queueName(queueName)
                .build();
        sqsClient.createQueue(request);

        final GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build();

        return sqsClient.getQueueUrl(getQueueRequest).queueUrl();
    }

    public void listQueues() {
        try {
            final ListQueuesIterable listQueues = sqsClient.listQueuesPaginator();
            listQueues.stream()
                    .flatMap(r -> r.queueUrls().stream())
                    .forEach(content -> System.out.println(" Queue URL: " + content.toLowerCase()));

        } catch (final SqsException e) {
            System.err.println(e.awsErrorDetails().errorMessage());
            System.exit(1);
        }
    }
}
