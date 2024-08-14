package com.hpe.caf.worker.queue.sqs;

import com.hpe.caf.worker.queue.sqs.config.SQSWorkerQueueConfiguration;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.ChangeMessageVisibilityRequest;

public class VisibilityTimeoutExtender
{
    private final SqsClient sqsClient;
    private final SQSWorkerQueueConfiguration sqsQueueConfiguration;

    public VisibilityTimeoutExtender(SqsClient sqsClient, SQSWorkerQueueConfiguration sqsQueueConfiguration)
    {
        this.sqsClient = sqsClient;
        this.sqsQueueConfiguration = sqsQueueConfiguration;
    }

    public void extendVisibility(final SQSTaskInformation taskInfo)
    {
        extendVisibility(taskInfo, sqsQueueConfiguration.getVisibilityTimeout() * 2);
    }

    // DDD some more thought on how this would be calculated/detected as required.
    public void extendVisibility(final SQSTaskInformation taskInfo, final int timeout)
    {
        var request = ChangeMessageVisibilityRequest.builder()
                .queueUrl(taskInfo.getQueueInfo().url())
                .receiptHandle(taskInfo.getReceiptHandle())
                .visibilityTimeout(timeout)
                .build();
        var result = sqsClient.changeMessageVisibility(request);
    }
}
