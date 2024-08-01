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

import com.hpe.caf.api.worker.TaskCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

public class SQSWorkerQueueConsumerImpl implements SQSQueueConsumer
{
    public static final String REJECTED_REASON_TASKMESSAGE = "TASKMESSAGE_INVALID";
    private final TaskCallback callback;
    private final BlockingQueue<SQSEvent<SQSQueueConsumer>> consumerEventQueue;
    private final BlockingQueue<SQSEvent<SQSWorkerPublisher>> publisherEventQueue;
    private final String retryQueue;
    private final int retryLimit;
    private static final Logger LOG = LoggerFactory.getLogger(SQSWorkerQueueConsumerImpl.class);
    // DDD how are metrics handles used?


    public SQSWorkerQueueConsumerImpl(
            final TaskCallback callback,
            final BlockingQueue<SQSEvent<SQSQueueConsumer>> consumerEventQueue,
            final BlockingQueue<SQSEvent<SQSWorkerPublisher>> publisherEventQueue,
            final String retryQueue,
            final int retryLimit)
    {
        this.callback = callback;
        this.consumerEventQueue = consumerEventQueue;
        this.publisherEventQueue = publisherEventQueue;
        this.retryQueue = retryQueue;
        this.retryLimit = retryLimit;
    }

    @Override
    public void processDelivery(final Object message)
    {
        // DDD How are retries reported for SQS
        // DDD metrics would be updated here


    }

    @Override
    public void processAck(final long tag)
    {

    }

    @Override
    public void processReject(final long tag)
    {

    }

    @Override
    public void processDrop(final long tag)
    {

    }
}
