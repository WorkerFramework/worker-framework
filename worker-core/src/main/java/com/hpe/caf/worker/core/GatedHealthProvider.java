/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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

    public GatedHealthProvider(final ManagedWorkerQueue workerQueue)
    {
        this.unhealthySet = new HashSet<>();
        this.workerQueue = workerQueue;
        this.healthCheckLock = new Object();
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
                    unhealthySet.add(name);

                    LOG.debug("Disconnecting the incoming queue due to the [{}] health check failing", name);
                    workerQueue.disconnectIncoming();
                } else if (!unhealthySet.isEmpty() && unhealthySet.remove(name) && unhealthySet.isEmpty()) {
                    LOG.debug("Reconnecting the incoming queue as all health checks now passing again");
                    workerQueue.reconnectIncoming();
                }

                return result;
            }
        }
    }
}
