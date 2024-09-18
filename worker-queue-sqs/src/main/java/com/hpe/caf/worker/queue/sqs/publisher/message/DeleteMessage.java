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

import com.hpe.caf.worker.queue.sqs.SQSTaskInformation;

import java.util.Objects;

public final class DeleteMessage
{
    private final SQSTaskInformation sqsTaskInformation;
    private int failedDeleteCount;

    public DeleteMessage(final SQSTaskInformation sqsTaskInformation)
    {
        this.sqsTaskInformation = sqsTaskInformation;
        failedDeleteCount = 0;
    }

    public SQSTaskInformation getSqsTaskInformation()
    {
        return sqsTaskInformation;
    }

    public int getFailedDeleteCount()
    {
        return failedDeleteCount;
    }

    public void incrementFailedDeleteCount()
    {
        failedDeleteCount++;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (DeleteMessage) obj;
        return Objects.equals(this.sqsTaskInformation, that.sqsTaskInformation);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(sqsTaskInformation);
    }

    @Override
    public String toString()
    {
        return "DeleteMessage[" +
                "failedDeleteCount=" + failedDeleteCount + ", " +
                "sqsTaskInformation=" + sqsTaskInformation + ']';
    }

}
