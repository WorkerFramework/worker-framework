package com.hpe.caf.worker.queue.sqs;

import software.amazon.awssdk.services.sqs.*;
import software.amazon.awssdk.services.sqs.model.*;

class SQSUtil
{
    static String getQueueUrl(final SqsClient sqsClient, final String queueName)
    {
        final var getQueueUrlRequest = GetQueueUrlRequest.builder()
                .queueName(queueName)
                .build();
        return sqsClient.getQueueUrl(getQueueUrlRequest).queueUrl();
    }
}
