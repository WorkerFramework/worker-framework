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

import com.hpe.caf.api.worker.TaskInformation;
import com.hpe.caf.worker.queue.sqs.visibility.VisibilityTimeout;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class SQSTaskInformation implements TaskInformation
{
    private static final Logger LOG = LoggerFactory.getLogger(SQSTaskInformation.class);

    @NotNull
    private final String inboundMessageId;

    @NotNull
    private final boolean isPoison;

    @NotNull
    private final VisibilityTimeout visibilityTimeout;

    private final AtomicBoolean wasLastMessageSent;
    private final AtomicInteger responseCount;
    private final AtomicInteger acknowledgementCount;

    public SQSTaskInformation(
            final String inboundMessageId,
            final VisibilityTimeout visibilityTimeout,
            final boolean isPoison)
    {
        this.inboundMessageId = inboundMessageId;
        this.visibilityTimeout = visibilityTimeout;
        this.isPoison = isPoison;
        wasLastMessageSent = new AtomicBoolean(false);
        this.responseCount = new AtomicInteger(0);
        this.acknowledgementCount = new AtomicInteger(0);
    }

    public QueueInfo getQueueInfo()
    {
        return visibilityTimeout.getQueueInfo();
    }

    @Override
    public String getInboundMessageId()
    {
        return inboundMessageId;
    }

    public VisibilityTimeout getVisibilityTimeout()
    {
        return visibilityTimeout;
    }

    @Override
    public boolean isPoison()
    {
        return isPoison;
    }

    public String getReceiptHandle()
    {
        return visibilityTimeout.getReceiptHandle();
    }

    public void incrementResponseCount(final boolean isLastMessage) {
        if (wasLastMessageSent.get()) {
            throw new RuntimeException("Final response already set!");
        }
        responseCount.incrementAndGet();
        LOG.debug("Incremented the responseCount for message:{} responseCount is: {}",
                visibilityTimeout.getReceiptHandle(), responseCount.get());
        orLastMessageSent(isLastMessage);
    }

    public void incrementAcknowledgementCount()
    {
        acknowledgementCount.incrementAndGet();
        LOG.debug("Incremented the acknowledgementCount for message:{} acknowledgementCount is: {}",
                visibilityTimeout.getReceiptHandle(), acknowledgementCount.get());
    }

    private void orLastMessageSent(final boolean isLastMessage)
    {
        wasLastMessageSent.compareAndExchange(false, isLastMessage);
    }

    public boolean processingComplete()
    {
        return wasLastMessageSent.get() && responseCount.get() == acknowledgementCount.get();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final SQSTaskInformation that = (SQSTaskInformation) o;
        return Objects.equals(visibilityTimeout.getReceiptHandle(), that.visibilityTimeout.getReceiptHandle());
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(visibilityTimeout.getReceiptHandle());
    }

    @Override
    public String toString()
    {
        return "SQSTaskInformation{" +
                "inboundMessageId='" + inboundMessageId + '\'' +
                ", isPoison=" + isPoison +
                ", visibilityTimeout=" + visibilityTimeout +
                ", processingComplete=" + processingComplete() +
                '}';
    }
}
