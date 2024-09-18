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
package com.hpe.caf.worker.queue.sqs.publisher.message;

import com.hpe.caf.worker.queue.sqs.QueueInfo;
import com.hpe.caf.worker.queue.sqs.SQSTaskInformation;

import java.util.Map;
import java.util.Objects;

public final class WorkerMessage
{
    private final QueueInfo queueInfo;
    private final byte[] taskMessage;
    private final Map<String, Object> headers;
    private final SQSTaskInformation inboundTaskInfo;

    public WorkerMessage(
            final QueueInfo queueInfo,
            final byte[] taskMessage,
            final Map<String, Object> headers,
            final SQSTaskInformation inboundTaskInfo)
    {
        this.queueInfo = queueInfo;
        this.taskMessage = taskMessage;
        this.headers = headers;
        this.inboundTaskInfo = inboundTaskInfo;
    }


    public QueueInfo getQueueInfo()
    {
        return queueInfo;
    }

    public byte[] getTaskMessage()
    {
        return taskMessage;
    }

    public Map<String, Object> getHeaders()
    {
        return headers;
    }

    public SQSTaskInformation getInboundTaskInfo()
    {
        return inboundTaskInfo;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (WorkerMessage) obj;
        return Objects.equals(this.queueInfo, that.queueInfo) &&
                Objects.equals(this.taskMessage, that.taskMessage) &&
                Objects.equals(this.headers, that.headers);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(queueInfo, taskMessage, headers);
    }

    @Override
    public String toString()
    {
        return "WorkerMessage[" +
                "queueInfo=" + queueInfo + ", " +
                "taskMessage=" + taskMessage + ", " +
                "inboundTaskInfo=" + inboundTaskInfo + ", " +
                "headers=" + headers + ']';
    }

}
