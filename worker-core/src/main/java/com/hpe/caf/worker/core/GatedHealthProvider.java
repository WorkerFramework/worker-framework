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

import java.util.HashSet;
import java.util.Set;

import com.hpe.caf.api.worker.ManagedWorkerQueue;

public class GatedHealthProvider
{

    private Set<String> unhealthySet;
    private ManagedWorkerQueue managedWorkerQueue;
    
    public GatedHealthProvider(ManagedWorkerQueue managedWorkerQueue)
    {
        this.managedWorkerQueue = managedWorkerQueue;
        this.unhealthySet = new HashSet<>();
    }

    public Set<String> getUnhealthySet()
    {
        return unhealthySet;
    }

    public void setUnhealthyCount(Set<String >unhealthySet)
    {
        this.unhealthySet = unhealthySet;
    }
    
    public void addUnhealthy(String unhealthyItem) {
        if (null == unhealthySet) {
            unhealthySet = new HashSet<>();
        }
        this.unhealthySet.add(unhealthyItem);
    }
    
    public void removeHealthy(String healthyItem) {
        if (null == unhealthySet) {
            unhealthySet = new HashSet<>();
        }
        this.unhealthySet.remove(healthyItem);
    }

    public ManagedWorkerQueue getManagedWorkerQueue()
    {
        return managedWorkerQueue;
    }

    public void setManagedWorkerQueue(ManagedWorkerQueue managedWorkerQueue)
    {
        this.managedWorkerQueue = managedWorkerQueue;
    }

}
