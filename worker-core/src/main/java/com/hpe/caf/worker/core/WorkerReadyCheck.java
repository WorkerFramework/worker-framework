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
package com.hpe.caf.worker.core;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.health.HealthCheck;
import com.hpe.caf.api.HealthReporter;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;

class WorkerReadyCheck extends HealthCheck
{
    private static final Logger LOG = LoggerFactory.getLogger(WorkerReadyCheck.class);

    private final HealthReporter reporter;

    public WorkerReadyCheck(final HealthReporter healthReporter)
    {
        this.reporter = Objects.requireNonNull(healthReporter);
    }

    @Override
    protected Result check()
        throws Exception
    {
        LOG.error("WorkerReadyCheck.check() called");
        HealthResult result = reporter.checkReady();
        String message = result.getMessage();

        if (result.getStatus() == HealthStatus.HEALTHY) {
            return message == null ? Result.healthy() : Result.healthy(message);
        } else {
            return Result.unhealthy(message);
        }
    }
}
