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

import com.hpe.caf.api.Configuration;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Configuration
public class SQSWorkerQueueConfiguration
{
    public SQSWorkerQueueConfiguration()
    {
    }

    @NotNull
    @Valid
    @Configuration
    private SQSConfiguration sqsConfiguration;

    /**
     * The queue to retrieve messages from.
     */
    @NotNull
    @Size(min = 1)
    private String inputQueue;

    /**
     * The queue to re-queue messages on.
     */
    @NotNull
    @Size(min = 1)
    private String retryQueue;

    /**
     * The duration (in seconds) for which the call waits for a message to arrive in the queue before returning.
     */
    @Min(0)
    @Max(20)
    private Integer longPollInterval;

    /**
     * The maximum number of messages to return when polling. Amazon SQS never returns more messages than this value
     * (however, fewer messages might be returned). Valid values: 1 to 10.
     */
    @Min(1)
    @Max(10)
    private Integer maxNumberOfMessages;

    /**
     * Immediately after a message is received, it remains in the queue. To prevent other consumers from
     * processing the message again, Amazon SQS sets a visibility timeout, a period of time during which
     * Amazon SQS prevents all consumers from receiving and processing the message.
     * <p>
     * The default visibility timeout for a message is 30 seconds. The minimum is 0 seconds. The maximum is 12 hours
     */
    @Min(30)
    @Max(43200)
    private Integer visibilityTimeout;

    /**
     * Immediately after a message is received, it remains in the dead letter queue. To prevent other consumers from
     * processing the message again, Amazon SQS sets a visibility timeout, a period of time during which
     * Amazon SQS prevents all consumers from receiving and processing the message.
     * <p>
     * The default visibility timeout for a message is 30 seconds. The minimum is 0 seconds. The maximum is 12 hours
     */
    @Min(30)
    @Max(43200)
    private Integer dlqVisibilityTimeout;

    /**
     * The queue to put messages sent to a paused worker on. If this is null, messages sent to a paused worker will be processed as
     * normal (as if the worker was not paused).
     */
    @Size(min = 1)
    private String pausedQueue;

    /**
     * The length of time, in seconds, for which Amazon SQS retains a message.
     * Valid values: An integer representing seconds, from 60 (1 minute) to 1,209,600 (14 days).
     *
     * Default is 345600(4 days)
     */
    @Min(60)
    @Max(1209600)
    private Integer messageRetentionPeriod;

    /**
     * The length of time, in seconds, for which Amazon SQS retains a message.
     * Valid values: An integer representing seconds, from 60 (1 minute) to 1,209,600 (14 days).
     *
     * Default is 345600(4 days)
     */
    @Min(60)
    @Max(1209600)
    private Integer dlqMessageRetentionPeriod;

    /**
     * The number of times a message will be delivered before being moved to the local dead-letter queue.
     *
     */
    @Min(0)
    private Integer maxDeliveries;

    public SQSConfiguration getSQSConfiguration()
    {
        return sqsConfiguration;
    }

    public void setSQSConfiguration(final SQSConfiguration sqsConfiguration)
    {
        this.sqsConfiguration = sqsConfiguration;
    }

    public String getInputQueue()
    {
        return inputQueue;
    }

    public void setInputQueue(final String inputQueue)
    {
        this.inputQueue = inputQueue;
    }

    public Integer getVisibilityTimeout()
    {
        return visibilityTimeout;
    }

    public void setVisibilityTimeout(final Integer visibilityTimeout)
    {
        this.visibilityTimeout = visibilityTimeout;
    }

    public Integer getLongPollInterval()
    {
        return longPollInterval;
    }

    public void setLongPollInterval(final Integer longPollInterval)
    {
        this.longPollInterval = longPollInterval;
    }

    public Integer getMaxNumberOfMessages()
    {
        return maxNumberOfMessages;
    }

    public void setMaxNumberOfMessages(final Integer maxNumberOfMessages)
    {
        this.maxNumberOfMessages = maxNumberOfMessages;
    }

    public Integer getMessageRetentionPeriod()
    {
        return messageRetentionPeriod;
    }

    public void setMessageRetentionPeriod(final Integer messageRetentionPeriod)
    {
        this.messageRetentionPeriod = messageRetentionPeriod;
    }

    public Integer getMaxDeliveries()
    {
        return maxDeliveries;
    }

    public void setMaxDeliveries(final Integer maxDeliveries)
    {
        this.maxDeliveries = maxDeliveries;
    }

    public Integer getDlqVisibilityTimeout()
    {
        return dlqVisibilityTimeout;
    }

    public void setDlqVisibilityTimeout(final Integer dlqVisibilityTimeout)
    {
        this.dlqVisibilityTimeout = dlqVisibilityTimeout;
    }

    public Integer getDlqMessageRetentionPeriod()
    {
        return dlqMessageRetentionPeriod;
    }

    public void setDlqMessageRetentionPeriod(final Integer dlqMessageRetentionPeriod)
    {
        this.dlqMessageRetentionPeriod = dlqMessageRetentionPeriod;
    }

    public String getRetryQueue()
    {
        return retryQueue;
    }

    public void setRetryQueue(final String retryQueue)
    {
        this.retryQueue = retryQueue;
    }
}

