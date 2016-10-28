package com.hpe.caf.util.rabbitmq;


/**
 * CAF RabbitMQ headers
 */
public class RabbitHeaders
{
    public static final String RABBIT_HEADER_CAF_WORKER_REJECTED = "x-caf-worker-rejected";
    public static final String RABBIT_HEADER_CAF_WORKER_RETRY = "x-caf-worker-retry";
    public static final String RABBIT_HEADER_CAF_WORKER_RETRY_LIMIT = "x-caf-worker-retry-limit";
}