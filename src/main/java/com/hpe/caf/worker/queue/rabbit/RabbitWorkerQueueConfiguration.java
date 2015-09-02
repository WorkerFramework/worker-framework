package com.hpe.caf.worker.queue.rabbit;


import com.hpe.caf.api.Configuration;
import com.hpe.caf.util.rabbitmq.RabbitConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;


public class RabbitWorkerQueueConfiguration
{
    /**
     * The base prefetch is determined by the number of tasks a microservice will run
     * simultaneously (which is dictated by the WorkerFactory) but we can buffer extra
     * ones with this parameter.
     */
    @Min(0)
    @Max(1000)
    private int prefetchBuffer;
    /**
     * The exchange to put dead letters (failed tasks) on. All tasks will be retried once, but if a task
     * fails that is already marked as redelivered, then it will be dumped here.
     */
    @NotNull
    @Size(min = 1)
    private String deadLetterExchange;
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


    public RabbitWorkerQueueConfiguration() { }


    public int getPrefetchBuffer()
    {
        return prefetchBuffer;
    }


    public void setPrefetchBuffer(final int prefetchBuffer)
    {
        this.prefetchBuffer = prefetchBuffer;
    }


    public String getDeadLetterExchange()
    {
        return deadLetterExchange;
    }


    public void setDeadLetterExchange(final String deadLetterExchange)
    {
        this.deadLetterExchange = deadLetterExchange;
    }


    public RabbitConfiguration getRabbitConfiguration()
    {
        return rabbitConfiguration;
    }


    public void setRabbitConfiguration(final RabbitConfiguration rabbitConfiguration)
    {
        this.rabbitConfiguration = rabbitConfiguration;
    }


    public String getInputQueue()
    {
        return inputQueue;
    }


    public void setInputQueue(final String inputQueue)
    {
        this.inputQueue = inputQueue;
    }
}
