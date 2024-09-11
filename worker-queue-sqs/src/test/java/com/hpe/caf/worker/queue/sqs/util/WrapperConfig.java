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
package com.hpe.caf.worker.queue.sqs.util;

public final class WrapperConfig
{
    private final int visibilityTimout;
    private final int longPollInterval;
    private final int maxReadMessages;
    private final int maxDeliveries;
    private final int retentionPeriod;
    private final int maxInflightMessages;

    public WrapperConfig()
    {
        this(30,
             5,
             1,
             1000,
             1000,
             120000);
    }

    public WrapperConfig(
            int visibilityTimout,
            int longPollInterval,
            int maxReadMessages,
            int maxDeliveries,
            int retentionPeriod,
            int maxInflightMessages)
    {
        this.visibilityTimout = visibilityTimout;
        this.longPollInterval = longPollInterval;
        this.maxReadMessages = maxReadMessages;
        this.maxDeliveries = maxDeliveries;
        this.retentionPeriod = retentionPeriod;
        this.maxInflightMessages = maxInflightMessages;
    }

    public int visibilityTimout()
    {
        return visibilityTimout;
    }

    public int longPollInterval()
    {
        return longPollInterval;
    }

    public int maxReadMessages()
    {
        return maxReadMessages;
    }

    public int maxDeliveries()
    {
        return maxDeliveries;
    }

    public int retentionPeriod()
    {
        return retentionPeriod;
    }

    public int maxInflightMessages()
    {
        return maxInflightMessages;
    }


}
