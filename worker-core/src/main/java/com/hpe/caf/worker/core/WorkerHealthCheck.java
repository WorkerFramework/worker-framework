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

import com.codahale.metrics.health.HealthCheck;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;

import java.util.Objects;
import java.util.function.Supplier;

class WorkerHealthCheck extends HealthCheck
{
    private final Supplier<HealthResult> healthResultSupplier;

    public WorkerHealthCheck(final Supplier<HealthResult> healthResultSupplier)
    {
        this.healthResultSupplier = Objects.requireNonNull(healthResultSupplier);
    }

    @Override
    protected Result check()
        throws Exception
    {
        HealthResult result = healthResultSupplier.get();
        String message = result.getMessage();

        if (result.getStatus() == HealthStatus.HEALTHY) {
            return message == null ? Result.healthy() : Result.healthy(message);
        } else {
            return Result.unhealthy(message);
        }
    }
}
