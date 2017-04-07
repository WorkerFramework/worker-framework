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
package com.hpe.caf.worker.testing.api;

/**
 * Created by ploch on 07/03/2017.
 */
public class WorkerInfo {

    private final int apiVersion;
    private final String workerName;
    private final Class workerTaskClass;
    private final Class workerResultClass;

    public WorkerInfo(int apiVersion, String workerName, Class workerTaskClass, Class workerResultClass) {

        this.apiVersion = apiVersion;
        this.workerName = workerName;
        this.workerTaskClass = workerTaskClass;
        this.workerResultClass = workerResultClass;
    }

    public int getApiVersion() {
        return apiVersion;
    }

    public String getWorkerName() {
        return workerName;
    }

    public Class getWorkerTaskClass() {
        return workerTaskClass;
    }

    public Class getWorkerResultClass() {
        return workerResultClass;
    }
}
