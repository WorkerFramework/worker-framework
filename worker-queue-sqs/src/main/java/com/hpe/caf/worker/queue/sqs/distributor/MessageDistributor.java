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
package com.hpe.caf.worker.queue.sqs.distributor;

import com.google.common.collect.Iterables;
import com.hpe.caf.worker.queue.sqs.QueueInfo;
import com.hpe.caf.worker.queue.sqs.util.SQSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.BatchResultErrorEntry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageSystemAttributeName;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResultEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public final class MessageDistributor
{
    private static final int SQS_MAX_BATCH_SIZE = 10;

    private final SqsClient sqsClient;
    private final QueueInfo source;
    private final QueueInfo destination;

    private static final Logger LOG = LoggerFactory.getLogger(MessageDistributor.class);

    public MessageDistributor(
            final  SqsClient sqsClient,
            final String source,
            final String destination
    )
    {
        this.sqsClient = sqsClient;
        this.source = SQSUtil.getQueueInfo(sqsClient, source);
        this.destination = SQSUtil.getQueueInfo(sqsClient, destination);
    }

    public List<BatchResultErrorEntry> moveMessages(final int maxMessages)
    {
        final var failures = new ArrayList<BatchResultErrorEntry>();
        final var messages = receive(maxMessages);
        if (messages.isEmpty()) {
            LOG.debug("No messages found to redistribute on queue {}", source.name());
            return new ArrayList<>();
        }
        // We need to look up the original message to delete.
        final var sourceMessageMap = messages.stream().collect(Collectors.toMap(Message::messageId, msg -> msg));

        final var sendBatches = Iterables.partition(messages, SQS_MAX_BATCH_SIZE);
        final var successfulSends = new ArrayList<SendMessageBatchResultEntry>();
        for (final var sendBatch : sendBatches) {
            try {
                final var sendMessageBatchResponse = send(sendBatch);
                successfulSends.addAll(sendMessageBatchResponse.successful());
                failures.addAll(sendMessageBatchResponse.failed());
            } catch (final Exception e) {
                LOG.error("Error sending a batch of messages", e);
            }
        }

        final var receiptHandles = successfulSends.stream()
                .map(ss -> sourceMessageMap.get(ss.id()).receiptHandle())
                .collect(Collectors.toSet());
        final var deleteBatches = Iterables.partition(receiptHandles, SQS_MAX_BATCH_SIZE);
        for (final var deleteBatch : deleteBatches) {
            try {
                var deleteMessageBatchResponse = delete(deleteBatch);
                failures.addAll(deleteMessageBatchResponse.failed());
            } catch (final Exception e) {
                LOG.error("Error deleting a batch of messages", e);
            }
        }
        return failures;
    }

    /**
     * The only guarantee is that we would not receive more than SQS_MAX_BATCH_SIZE messages per request.
     *
     * @return A list of messages
     */
    private List<Message> receive(final int maxMessages)
    {
        final List<Message> receivedMessages = new ArrayList<>();
        for (int i = 0; i < maxMessages / SQS_MAX_BATCH_SIZE; i++) {
            final var receiveRequest = ReceiveMessageRequest.builder()
                    .queueUrl(source.url())
                    .maxNumberOfMessages(SQS_MAX_BATCH_SIZE)
                    .waitTimeSeconds(0)
                    .messageSystemAttributeNames(MessageSystemAttributeName.ALL)
                    .messageAttributeNames(SQSUtil.ALL_ATTRIBUTES)
                    .build();
            receivedMessages.addAll(sqsClient.receiveMessage(receiveRequest).messages());
        }
        return receivedMessages;
    }

    private SendMessageBatchResponse send(final Collection<Message> messages)
    {
        final var entries = messages
                .stream()
                .map(msg -> SendMessageBatchRequestEntry.builder()
                        .id(msg.messageId())
                        .messageAttributes(msg.messageAttributes())
                        .delaySeconds(0)
                        .messageBody(msg.body())
                        .build())
                .collect(Collectors.toList());
        return sendMessageBatch(entries);
    }

    private DeleteMessageBatchResponse delete(final List<String> receiptHandles)
    {
        var id = 1;
        final var entries = new ArrayList<DeleteMessageBatchRequestEntry>();
        for(final String receiptHandle : receiptHandles) {
            entries.add(DeleteMessageBatchRequestEntry.builder()
                    .id(String.valueOf(id++))
                    .receiptHandle(receiptHandle)
                    .build());
        }
        return deleteMessageBatch(entries);
    }

    private SendMessageBatchResponse sendMessageBatch(final List<SendMessageBatchRequestEntry> entries)
    {
        final var sendRequest = SendMessageBatchRequest.builder()
                .queueUrl(destination.url())
                .entries(entries)
                .build();
        return sqsClient.sendMessageBatch(sendRequest);
    }

    private DeleteMessageBatchResponse deleteMessageBatch(final List<DeleteMessageBatchRequestEntry> entries)
    {
        final var deleteRequest = DeleteMessageBatchRequest.builder()
                .queueUrl(source.url())
                .entries(entries)
                .build();
        return sqsClient.deleteMessageBatch(deleteRequest);
    }
}
