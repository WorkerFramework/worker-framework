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
package com.hpe.caf.worker.queue.sqs.visibility;

import com.hpe.caf.worker.queue.sqs.QueueInfo;

import java.util.Objects;

public record VisibilityTimeout(QueueInfo queueInfo,
                                Long becomesVisibleEpochSecond,
                                String receiptHandle) implements Comparable<VisibilityTimeout>
{
    @Override
    public int compareTo(final VisibilityTimeout o)
    {
        return becomesVisibleEpochSecond.compareTo(o.becomesVisibleEpochSecond);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final VisibilityTimeout that = (VisibilityTimeout) o;
        return Objects.equals(receiptHandle, that.receiptHandle);
    }

    @Override
    public int hashCode()
    {
        return Objects.hashCode(receiptHandle);
    }

    @Override
    public String toString()
    {
        return "VisibilityTimeout{" +
                "becomesVisibleEpochSecond=" + becomesVisibleEpochSecond +
                ", receiptHandle='" + receiptHandle + '\'' +
                '}';
    }
}