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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hpe.caf.api.HealthReporter;
import com.hpe.caf.api.HealthResult;
import com.hpe.caf.api.HealthStatus;

public class TransientHealthCheck implements HealthReporter
{
    private static final Logger LOG = LoggerFactory.getLogger(TransientHealthCheck.class);
    
    private Map<String, LocalDateTime> transientExceptionRegistry = new HashMap<>();
    private Object transientExceptionRegistryLock = new Object();
    private long elapsedTime = 30;
    
    /**
     * This method checks if any entry in the Transient Exception Registry is newer than the time
     * now, minus the elapsed time interval. If an entry is found within the range the health check
     * returns Unhealthy. If the Transient Exception Registry is empty or no entries are found
     * within the range the health check returns Healthy.
     * 
     * @return HealthResult the result of the Transient Health Check, Healthy or Unhealthy
     */
    @Override
    public HealthResult healthCheck()
    {
        LOG.debug("Transient Health Check executing");
        HealthResult healthResult = HealthResult.RESULT_HEALTHY;
        synchronized (transientExceptionRegistryLock) {
            for(Map.Entry<String, LocalDateTime> entry : transientExceptionRegistry.entrySet()) {
                
                if(entry.getValue().toEpochSecond(ZoneOffset.UTC) < LocalDateTime.now().minusSeconds(elapsedTime).toEpochSecond(ZoneOffset.UTC)) {
                    
                    healthResult = HealthResult.RESULT_HEALTHY;
                } else {
                    healthResult = new HealthResult(HealthStatus.UNHEALTHY);
                    break;
                }
            }
            if(healthResult.equals(HealthResult.RESULT_HEALTHY)) {
                LOG.debug("Transient Health Check is currently Healthy, therefore clearing the Transient Exception Registry");
                transientExceptionRegistry.clear();
            }
            LOG.debug("Transient Health Check is currently [{}]", healthResult.getMessage());
            return healthResult;
        }
    }
    
    /**
     * This method adds the supplied exceptionMsg text as a key to a Map and adds the time now as
     * the corresponding value. This Map represents the Transient Exception Registry, which is use
     * to store Transient Exceptions.
     * 
     * @param exceptionMsg Adds the exception message text
     */
    public void addTransientExceptionToRegistry(String exceptionMsg)
    {
        LocalDateTime now = LocalDateTime.now();
        LOG.debug("Adding the following exception and time to the Transient Exception Registry [{}, {}]", exceptionMsg, now);
        synchronized (transientExceptionRegistryLock) {
            transientExceptionRegistry.put(exceptionMsg, now);
        }
    }

}
