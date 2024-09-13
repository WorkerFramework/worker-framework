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

import java.util.Objects;

public final class QueueInfo
{
    private final String name;
    private final String url;
    private final String arn;

    public QueueInfo(
            final String name,
            final String url,
            final String arn
    )
    {
        this.name = name;
        this.url = url;
        this.arn = arn;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final QueueInfo queueInfo = (QueueInfo) o;
        return  Objects.equals(url, queueInfo.url) &&
                Objects.equals(arn, queueInfo.arn) &&
                Objects.equals(name, queueInfo.name);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name, url, arn);
    }

    public String name()
    {
        return name;
    }

    public String url()
    {
        return url;
    }

    public String arn()
    {
        return arn;
    }

    @Override
    public String toString()
    {
        return "QueueInfo[" +
                "name=" + name + ", " +
                "url=" + url + ", " +
                "arn=" + arn + ']';
    }

}
