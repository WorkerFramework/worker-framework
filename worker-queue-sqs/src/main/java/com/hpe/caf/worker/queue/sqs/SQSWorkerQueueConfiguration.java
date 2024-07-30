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
     * The queue to put messages sent to a paused worker on. If this is null, messages sent to a paused worker will be processed as
     * normal (as if the worker was not paused).
     */
    @Size(min = 1)
    private String pausedQueue;

    /**
     * The queue to put redelivered messages on. If this null, the inputQueue will be used.
     */
    private String retryQueue;

    /**
     * The queue to put rejected messages on.
     */
    @Deprecated
    @NotNull
    @Size(min = 1)
    private String rejectedQueue;

    /**
     * The maximum number of times for redelivered messages to be retried before moving them to the rejectedQueue. This does not include
     * messages explicitly rejected by the Worker at delivery time.
     */
    @Min(1)
    private int retryLimit;

    /**
     * The maximum message priority supported by this worker queue. 0 to disable priority feature.
     */
    @Min(0)
    @Max(255)
    private int maxPriority;

    /**
     * The type of queues to create, can currently be either quorum or classic
     */
    @NotNull
    private String queueType;


    public SQSConfiguration getSQSConfiguration()
    {
        return sqsConfiguration;
    }

    public void setSQSConfiguration(SQSConfiguration sqsConfiguration)
    {
        this.sqsConfiguration = sqsConfiguration;
    }

    public String getInputQueue()
    {
        return inputQueue;
    }

    public void setInputQueue(String inputQueue)
    {
        this.inputQueue = inputQueue;
    }

    public String getPausedQueue()
    {
        return pausedQueue;
    }

    public void setPausedQueue(String pausedQueue)
    {
        this.pausedQueue = pausedQueue;
    }

    public String getRetryQueue()
    {
        return retryQueue == null ? inputQueue : retryQueue;
    }

    public void setRetryQueue(String retryQueue)
    {
        this.retryQueue = retryQueue;
    }

    @Deprecated
    public String getRejectedQueue()
    {
        return rejectedQueue;
    }

    @Deprecated
    public void setRejectedQueue(String rejectedQueue)
    {
        this.rejectedQueue = rejectedQueue;
    }

    public int getRetryLimit()
    {
        return retryLimit;
    }

    public void setRetryLimit(int retryLimit)
    {
        this.retryLimit = retryLimit;
    }

    public int getMaxPriority()
    {
        return maxPriority;
    }

    public void setMaxPriority(int maxPriority) {
        this.maxPriority = maxPriority;
    }

    public String getQueueType()
    {
        return queueType;
    }

    public void setQueueType(String queueType)
    {
        this.queueType = queueType;
    }
}

