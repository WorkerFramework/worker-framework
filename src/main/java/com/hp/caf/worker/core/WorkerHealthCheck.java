package com.hp.caf.worker.core;


import com.codahale.metrics.health.HealthCheck;
import com.hp.caf.api.HealthReporter;
import com.hp.caf.api.HealthResult;
import com.hp.caf.api.HealthStatus;

import java.util.Objects;


class WorkerHealthCheck extends HealthCheck
{
    private final HealthReporter reporter;


    public WorkerHealthCheck(final HealthReporter healthReporter)
    {
        this.reporter = Objects.requireNonNull(healthReporter);
    }


    @Override
    protected Result check()
            throws Exception
    {
        HealthResult result = reporter.healthCheck();
        String message = result.getMessage();

        if ( result.getStatus() == HealthStatus.HEALTHY ) {
            return message == null ? Result.healthy() : Result.healthy(message);
        } else {
            return Result.unhealthy(message);
        }
    }
}
