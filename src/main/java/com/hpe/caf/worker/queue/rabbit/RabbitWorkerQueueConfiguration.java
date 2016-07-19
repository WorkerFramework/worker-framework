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
 * @since 7.5
 */
@Configuration
public class RabbitWorkerQueueConfiguration
{
    /**
     * The base prefetch is determined by the number of tasks a microservice will run
     * simultaneously (which is dictated by the WorkerFactory) but we can buffer extra
     * ones with this parameter.
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
     * The queue to put redelivered messages on. If this null, the inputQueue will be used.
     * @since 10.6
     */
    private String retryQueue;
    /**
     * The queue to put rejected messages on.
     * @since 10.6
     */
    @NotNull
    @Size(min = 1)
    private String rejectedQueue;
    /**
     * The maximum number of times for redelivered messages to be retried before moving them to the rejectedQueue.
     * This does not include messages explicitly rejected by the Worker at delivery time.
     * @since 10.6
     */
    @Min(1)
    private int retryLimit;


    public RabbitWorkerQueueConfiguration() { }


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


    public String getRetryQueue()
    {
        return retryQueue == null ? inputQueue : retryQueue;
    }


    public void setRetryQueue(String retryQueue)
    {
        this.retryQueue = retryQueue;
    }


    public String getRejectedQueue()
    {
        return rejectedQueue;
    }


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
