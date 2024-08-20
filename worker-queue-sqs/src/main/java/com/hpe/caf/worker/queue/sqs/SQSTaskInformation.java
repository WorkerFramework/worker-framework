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
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Objects;

public final class SQSTaskInformation implements TaskInformation, Comparable<SQSTaskInformation>
{
    @NotNull
    private final QueueInfo queueInfo;

    @NotNull
    private final String inboundMessageId;

    @NotNull
    private final String receiptHandle;

    @NotNull
    private final boolean isPoison;

    @NotNull
    private final Instant becomesVisible;

    public SQSTaskInformation(
            final QueueInfo queueInfo,
            final String inboundMessageId,
            final String receiptHandle,
            final Instant becomesVisible,
            final boolean isPoison)
    {
        this.queueInfo = queueInfo;
        this.inboundMessageId = inboundMessageId;
        this.receiptHandle = receiptHandle;
        this.becomesVisible = becomesVisible;
        this.isPoison = isPoison;
    }

    public QueueInfo getQueueInfo()
    {
        return queueInfo;
    }

    @Override
    public String getInboundMessageId()
    {
        return inboundMessageId;
    }

    public String getReceiptHandle()
    {
        return receiptHandle;
    }

    public Instant getBecomesVisible()
    {
        return becomesVisible;
    }

    @Override
    public boolean isPoison()
    {
        return isPoison;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final SQSTaskInformation that = (SQSTaskInformation) o;
        return Objects.equals(receiptHandle, that.receiptHandle);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(receiptHandle);
    }

    @Override
    public int compareTo(@NotNull final SQSTaskInformation o)
    {
        return becomesVisible.compareTo(o.becomesVisible);
    }

    @Override
    public String toString()
    {
        return "SQSTaskInformation{" +
                "queueInfo=" + queueInfo +
                ", inboundMessageId='" + inboundMessageId + '\'' +
                ", receiptHandle='" + receiptHandle + '\'' +
                ", isPoison=" + isPoison +
                ", becomesVisible=" + becomesVisible +
                '}';
    }
}
