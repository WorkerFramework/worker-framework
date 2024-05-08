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
package com.hpe.caf.configs;

import jakarta.validation.constraints.Min;

/**
 * Configuration for the liveness and readiness checks.
 */
public class HealthConfiguration
{
    /**
     * The initial delay to use when first scheduling the liveness check.
     */
    @Min(0)
    private int livenessInitialDelaySeconds = 15;

    /**
     * The interval on which to perform a liveness check for while in a healthy state.
     */
    @Min(60)
    private int livenessCheckIntervalSeconds = 60;

    /**
     * The interval on which to perform a liveness check for while in an unhealthy state.
     */
    @Min(60)
    private int livenessDowntimeIntervalSeconds = 60;

    /**
     * The threshold of consecutive successful attempts needed to mark the liveness check as healthy (from an unhealthy state).
     */
    @Min(1)
    private int livenessSuccessAttempts = 1;

    /**
     * The threshold of consecutive failed attempts needed to mark the liveness check as unhealthy (from a healthy state).
     */
    @Min(1)
    private int livenessFailureAttempts = 3;

    /**
     * The initial delay to use when first scheduling the readiness check.
     */
    @Min(0)
    private int readinessInitialDelaySeconds = 15;

    /**
     * The interval on which to perform a readiness check for while in a healthy state.
     */
    @Min(1)
    private int readinessCheckIntervalSeconds = 60;

    /**
     * The interval on which to perform a readiness check for while in an unhealthy state.
     */
    @Min(1)
    private int readinessDowntimeIntervalSeconds = 60;

    /**
     * The threshold of consecutive successful attempts needed to mark the readiness check as healthy (from an unhealthy state).
     */
    @Min(1)
    private int readinessSuccessAttempts = 1;

    /**
     * The threshold of consecutive failed attempts needed to mark the readiness check as unhealthy (from a healthy state).
     */
    @Min(1)
    private int readinessFailureAttempts = 3;

    public int getLivenessInitialDelaySeconds()
    {
        return livenessInitialDelaySeconds;
    }

    public void setLivenessInitialDelaySeconds(final int livenessInitialDelaySeconds)
    {
        this.livenessInitialDelaySeconds = livenessInitialDelaySeconds;
    }

    public int getLivenessCheckIntervalSeconds()
    {
        return livenessCheckIntervalSeconds;
    }

    public void setLivenessCheckIntervalSeconds(final int livenessCheckIntervalSeconds)
    {
        this.livenessCheckIntervalSeconds = livenessCheckIntervalSeconds;
    }

    public int getLivenessDowntimeIntervalSeconds()
    {
        return livenessDowntimeIntervalSeconds;
    }

    public void setLivenessDowntimeIntervalSeconds(final int livenessDowntimeIntervalSeconds)
    {
        this.livenessDowntimeIntervalSeconds = livenessDowntimeIntervalSeconds;
    }

    public int getLivenessSuccessAttempts()
    {
        return livenessSuccessAttempts;
    }

    public void setLivenessSuccessAttempts(final int livenessSuccessAttempts)
    {
        this.livenessSuccessAttempts = livenessSuccessAttempts;
    }

    public int getLivenessFailureAttempts()
    {
        return livenessFailureAttempts;
    }

    public void setLivenessFailureAttempts(final int livenessFailureAttempts)
    {
        this.livenessFailureAttempts = livenessFailureAttempts;
    }

    public int getReadinessInitialDelaySeconds()
    {
        return readinessInitialDelaySeconds;
    }

    public void setReadinessInitialDelaySeconds(final int readinessInitialDelaySeconds)
    {
        this.readinessInitialDelaySeconds = readinessInitialDelaySeconds;
    }

    public int getReadinessCheckIntervalSeconds()
    {
        return readinessCheckIntervalSeconds;
    }

    public void setReadinessCheckIntervalSeconds(final int readinessCheckIntervalSeconds)
    {
        this.readinessCheckIntervalSeconds = readinessCheckIntervalSeconds;
    }

    public int getReadinessDowntimeIntervalSeconds()
    {
        return readinessDowntimeIntervalSeconds;
    }

    public void setReadinessDowntimeIntervalSeconds(final int readinessDowntimeIntervalSeconds)
    {
        this.readinessDowntimeIntervalSeconds = readinessDowntimeIntervalSeconds;
    }

    public int getReadinessSuccessAttempts()
    {
        return readinessSuccessAttempts;
    }

    public void setReadinessSuccessAttempts(final int readinessSuccessAttempts)
    {
        this.readinessSuccessAttempts = readinessSuccessAttempts;
    }

    public int getReadinessFailureAttempts()
    {
        return readinessFailureAttempts;
    }

    public void setReadinessFailureAttempts(final int readinessFailureAttempts)
    {
        this.readinessFailureAttempts = readinessFailureAttempts;
    }
}
