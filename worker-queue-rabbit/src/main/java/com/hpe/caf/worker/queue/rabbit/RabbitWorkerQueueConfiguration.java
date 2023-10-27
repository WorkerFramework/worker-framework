/*
 * Copyright 2015-2023 Open Text.
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
package com.hpe.caf.worker.queue.rabbit;

import com.hpe.caf.api.Configuration;
import com.hpe.caf.configs.RabbitConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Configuration for the worker-queue-rabbit module.
 */
@Configuration
public class RabbitWorkerQueueConfiguration
{
    /**
     * The base prefetch is determined by the number of tasks a microservice will run simultaneously (which is dictated by the
     * WorkerFactory) but we can buffer extra ones with this parameter.
     */
    @Min(0)
    @Max(100000)
    private int prefetchBuffer;
    /**
     * The internal RabbitMQ configuration itself.
     */
    @NotNull
    @Valid
    @Configuration
    private RabbitConfiguration rabbitConfiguration;
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

    public RabbitWorkerQueueConfiguration()
    {
    }

    public int getPrefetchBuffer()
    {
        return prefetchBuffer;
    }

    public void setPrefetchBuffer(int prefetchBuffer)
    {
        this.prefetchBuffer = prefetchBuffer;
    }

    public RabbitConfiguration getRabbitConfiguration()
    {
        return rabbitConfiguration;
    }

    public void setRabbitConfiguration(RabbitConfiguration rabbitConfiguration)
    {
        this.rabbitConfiguration = rabbitConfiguration;
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
}
