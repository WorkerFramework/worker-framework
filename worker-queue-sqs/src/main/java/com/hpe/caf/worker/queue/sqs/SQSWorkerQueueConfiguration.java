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
import com.hpe.caf.configs.SQSConfiguration;
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
     * The duration (in seconds) for which the call waits for a message to arrive in the queue before returning.
     */
    @Min(0)
    @Max(600)
    private int longPollInterval;

    /**
     * Immediately after a message is received, it remains in the queue. To prevent other consumers from
     * processing the message again, Amazon SQS sets a visibility timeout, a period of time during which
     * Amazon SQS prevents all consumers from receiving and processing the message.
     * <p>
     * The default visibility timeout for a message is 30 seconds. The minimum is 0 seconds. The maximum is 12 hours
     */
    @Min(30)
    @Max(43200)
    private int visibilityTimeout;

    /**
     * The queue to put messages sent to a paused worker on. If this is null, messages sent to a paused worker will be processed as
     * normal (as if the worker was not paused).
     */
    @Size(min = 1)
    private String pausedQueue;

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

    public int getVisibilityTimeout()
    {
        return visibilityTimeout;
    }

    public void setVisibilityTimeout(final int visibilityTimeout)
    {
        this.visibilityTimeout = visibilityTimeout;
    }

    public int getLongPollInterval()
    {
        return longPollInterval;
    }

    public void setLongPollInterval(final int longPollInterval)
    {
        this.longPollInterval = longPollInterval;
    }
}

