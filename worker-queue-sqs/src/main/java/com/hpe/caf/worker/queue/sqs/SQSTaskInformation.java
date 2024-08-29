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

public final class SQSTaskInformation implements TaskInformation
{
    @NotNull
    private final QueueInfo queueInfo;

    @NotNull
    private final String inboundMessageId;

    @NotNull
    private final boolean isPoison;

    @NotNull
    private final VisibilityTimeout visibilityTimeout;

    public SQSTaskInformation(
            final QueueInfo queueInfo,
            final String inboundMessageId,
            final VisibilityTimeout visibilityTimeout,
            final boolean isPoison)
    {
        this.queueInfo = queueInfo;
        this.inboundMessageId = inboundMessageId;
        this.visibilityTimeout = visibilityTimeout;
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
        return visibilityTimeout.receiptHandle();
    }

    @Override
    public String toString()
    {
        return "SQSTaskInformation{" +
                "queueInfo=" + queueInfo +
                ", inboundMessageId='" + inboundMessageId + '\'' +
                ", isPoison=" + isPoison +
                ", visibilityTimeout='" + visibilityTimeout + '\'' +
                '}';
    }
}
