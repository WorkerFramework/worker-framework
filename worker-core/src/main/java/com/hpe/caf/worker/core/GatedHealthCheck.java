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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.health.HealthCheck;
import com.hpe.caf.api.worker.ManagedWorkerQueue;

public class GatedHealthCheck extends HealthCheck
{
    private static final Logger LOG = LoggerFactory.getLogger(GatedHealthCheck.class);

    private GatedHealthProvider gatedHealthProvider;
    private HealthCheck healthCheck;
    private String name;

    public GatedHealthCheck(String name, GatedHealthProvider gatedHealthProvider, HealthCheck healthCheck)
    {
        this.name = name;
        this.gatedHealthProvider = gatedHealthProvider;
        this.healthCheck = healthCheck;
    }

    @Override
    /**
     * This method executes the available health checks and can disconnect or reconnect a worker to
     * it's input queue depending on the workers overall health.
     * 
     * If a health check fails, the health check is added to the set of unhealthy checks and the
     * worker is disconnected from it's input queue.
     * 
     * If a health check passes and the set of unhealthy checks was previously empty, nothing further
     * is required.
     * 
     * If a health check passes and the set of unhealthy checks is not empty, an attempt to remove
     * the passing health check from the set of unhealthy checks is made.
     * 
     * If the set of unhealthy checks was previously of size 1 and is still of size 1, a different
     * health check has previously failed and the work remains unhealthy and disconnected from it's
     * input queue.
     * 
     * If the set of unhealthy checks was previously of size 1 and is now of size 0, the worker is
     * healthy again and reconnected to it's input queue.
     */
    protected Result check() throws Exception
    {
        Result result = healthCheck.execute();
        ManagedWorkerQueue managedWorkerQueue = gatedHealthProvider.getManagedWorkerQueue();
        if (!result.isHealthy()) {
            // Add the name of the failed health check to the set of unhealthy checks
            gatedHealthProvider.addUnhealthy(name);
            LOG.debug("Disconnecting the incoming queue due to the [{}] health check failing", name);
            managedWorkerQueue.disconnectIncoming();
        } else {
            // Check the current number of unhealthy items
            int unhealthyItems = gatedHealthProvider.getUnhealthySet().size();
            if (unhealthyItems > 0) {
                gatedHealthProvider.removeHealthy(name);
                // If the previous number of unhealthy items was 1 and now there are no unhealthy items
                // reconnect the incoming queue to the worker
                if (unhealthyItems == 1 && gatedHealthProvider.getUnhealthySet().size() == 0) {
                    LOG.debug("Reconnecting the incoming queue as all health checks now passing again");
                    managedWorkerQueue.reconnectIncoming();
                }
            }
        }
        return result;
    }

}
