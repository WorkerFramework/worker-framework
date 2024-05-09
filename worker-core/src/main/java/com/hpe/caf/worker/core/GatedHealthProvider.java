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
import com.hpe.caf.api.worker.ManagedWorkerQueue;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GatedHealthProvider
{
    private static final Logger LOG = LoggerFactory.getLogger(GatedHealthProvider.class);

    private final Set<String> unhealthySet;
    private final ManagedWorkerQueue workerQueue;
    private final Object healthCheckLock;
    private final WorkerCore core;

    public GatedHealthProvider(final ManagedWorkerQueue workerQueue, final WorkerCore core)
    {
        this.unhealthySet = new HashSet<>();
        this.workerQueue = workerQueue;
        this.healthCheckLock = new Object();
        this.core = core;
    }

    public final class GatedHealthCheck extends HealthCheck
    {
        private final String name;
        private final HealthCheck healthCheck;

        public GatedHealthCheck(final String name, final HealthCheck healthCheck)
        {
            this.name = name;
            this.healthCheck = healthCheck;
        }

        /**
         * Disconnects the queue if any health check fails and re-connects it when the worker becomes healthy again.
         */
        @Override
        protected Result check() throws Exception
        {
            synchronized (healthCheckLock) {

                final Result result = healthCheck.execute();

                if (!result.isHealthy()) {
                    // Add the name of the failed health check to the set of unhealthy checks
                    LOG.warn("Health check failing: Name={}, Message={}, Details={}, Time={}, Timestamp={}, Duration={}, Error={}",
                            name, result.getMessage(), result.getDetails(), result.getTime(), result.getTimestamp(), result.getDuration(),
                            result.getError() != null ? result.getError().toString() : "null");
                    unhealthySet.add(name);
                } else if (!unhealthySet.isEmpty()) {
                    // Remove the name of the health check if it is present in the unhealthy set
                    unhealthySet.remove(name);
                }

                if (unhealthySet.isEmpty()) {
                    if(!core.isStarted())
                    {
                        LOG.debug("Starting WorkerCore...");
                        core.start();
                    }
                    else
                    {
                        LOG.debug("Reconnecting the incoming queue as all health checks are passing");
                        workerQueue.reconnectIncoming();
                    }
                } else {
                    LOG.debug("Disconnecting the incoming queue due to the {} health check(s) failing", unhealthySet);
                    workerQueue.disconnectIncoming();
                }

                return result;
            }
        }
    }
}
