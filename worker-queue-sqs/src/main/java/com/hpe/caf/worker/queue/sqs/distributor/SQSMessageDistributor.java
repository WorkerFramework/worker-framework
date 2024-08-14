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

import com.hpe.caf.worker.queue.sqs.QueueInfo;
import com.hpe.caf.worker.queue.sqs.SQSClientProvider;
import com.hpe.caf.worker.queue.sqs.SQSUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse;
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResultEntry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public final class SQSMessageDistributor
{
    private static final int SQS_MAX_BATCH_SIZE = 10;

    private final SqsClient sqsClient;
    private final QueueInfo source;
    private final QueueInfo destination;

    private static final Logger LOG = LoggerFactory.getLogger(SQSMessageDistributor.class);

    public SQSMessageDistributor(
            final SQSClientProvider provider,
            final String source,
            final String destination
    ) throws Exception
    {
        this.sqsClient = provider.getSqsClient();
        this.source = SQSUtil.getQueueInfo(sqsClient, source);
        this.destination = SQSUtil.getQueueInfo(sqsClient, destination);
    }

    public List<SendMessageBatchResultEntry> moveMessages(final int maxMessages)
    {
        final var movedMessages = new ArrayList<SendMessageBatchResultEntry>();
        for (int i = 0; i < maxMessages/SQS_MAX_BATCH_SIZE; i++)
        {
            try {
                final var optionalStringMessageMap = receive();
                if (optionalStringMessageMap.isEmpty()) {
                    LOG.info("No messages found to redistribute on queue {}", source.name());
                    break;
                }
                final var sourceMessageMap = optionalStringMessageMap.get();
                final var sendMessageBatchResponse = redistribute(sourceMessageMap.values());
                var deleteMessageBatchResponse = delete(sendMessageBatchResponse, sourceMessageMap);
                // DDD add failed deletes here?
                movedMessages.addAll(sendMessageBatchResponse.successful());
            } catch (final Exception e) {
                LOG.error("Error redistributing a batch of messages", e);
            }
        }
        return movedMessages;
    }

    private SendMessageBatchResponse redistribute(final List<Message> messages)
    {
        final var sendMessageBatchResponse = redistribute(messages);

        sendMessageBatchResponse.failed()
                .stream()
                .forEach(msg->{
                    LOG.error(msg.toString());
                });
        return sendMessageBatchResponse;
    }

    private Optional<Map<String, Message>> receive()
    {
        final var receiveRequest = ReceiveMessageRequest.builder()
                .queueUrl(source.url())
                .maxNumberOfMessages(SQS_MAX_BATCH_SIZE)
                .waitTimeSeconds(0)
                .attributeNamesWithStrings(SQSUtil.ALL_ATTRIBUTES)
                .messageAttributeNames(SQSUtil.ALL_ATTRIBUTES)
                .build();
        final var result = sqsClient.receiveMessage(receiveRequest).messages();
        final var messageMap = result.stream().collect(Collectors.toMap(msg->msg.messageId(), msg->msg));
        return Optional.of(messageMap);
    }

    private DeleteMessageBatchResponse delete(
            final SendMessageBatchResponse sendMessageBatchResponse,
            Map<String, Message> sourceMessageMap
    )
    {
        final var deleteMessageBatchRequestEntries = sendMessageBatchResponse.successful()
                .stream()
                .map(msg-> {
                    final var originalMessage = sourceMessageMap.get(msg.id());
                    final var entry = DeleteMessageBatchRequestEntry.builder()
                            .id(msg.messageId())
                            .receiptHandle(originalMessage.receiptHandle())
                            .build();
                    return entry;
                })
                .collect(Collectors.toList());
        final var deleteMessageBatchResponse = deleteMessageBatch(deleteMessageBatchRequestEntries);
        deleteMessageBatchResponse.failed()
                .stream()
                .forEach(msg->{
                    LOG.error(msg.toString());
                });
        return deleteMessageBatchResponse;
    }

    private SendMessageBatchResponse redistribute(final Collection<Message> messages)
    {
        final var entries = messages
                .stream()
                .map(msg->{
                    final var entry = SendMessageBatchRequestEntry.builder()
                            .id(msg.messageId())
                            .messageAttributes(msg.messageAttributes())
                            .delaySeconds(0)
                            .messageBody(msg.body())
                            .build();
                    return entry;
                })
                .collect(Collectors.toList());
        return sendMessageBatch(entries);
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
