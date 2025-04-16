/*
 * Copyright 2015-2025 Open Text.
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
package com.github.workerframework.configs;

import jakarta.validation.constraints.Min;

/**
 * General configuration for the threshold at which messages will be dehydrated before publishing to RabbitMQ,
 * and rehydrated when consumed by a worker.
 */
public class MessageDehydrationConfiguration
{
    /**
     * Indicates if message dehydration is enabled.
     */
    private boolean isEnabled = false;

    /**
     * The threshold at which messages will be dehydrated before publishing to RabbitMQ.
     */
    @Min(1)
    private int threshold = 16777216;

    /**
     * Indicates if message dehydration is enabled.
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    /**
     * The threshold at which messages will be dehydrated before publishing to RabbitMQ.
     */
    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }
}
