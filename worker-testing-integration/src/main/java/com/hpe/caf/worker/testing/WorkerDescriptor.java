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
package com.hpe.caf.worker.testing;

/**
 * Created by ploch on 08/11/2015.
 */
public class WorkerDescriptor<TConfiguration, TTask, TResult> {

    private final Class<TConfiguration> configurationClass;
    private final Class<TTask> taskClass;
    private final Class<TResult> resultClass;
    private final String workerName;
    private final int apiVersion;

    public WorkerDescriptor(Class<TConfiguration> configurationClass, Class<TTask> taskClass, Class<TResult> resultClass, String workerName, int apiVersion) {

        this.configurationClass = configurationClass;
        this.taskClass = taskClass;
        this.resultClass = resultClass;
        this.workerName = workerName;
        this.apiVersion = apiVersion;
    }

    /**
     * Getter for property 'configurationClass'.
     *
     * @return Value for property 'configurationClass'.
     */
    public Class<TConfiguration> getConfigurationClass() {
        return configurationClass;
    }

    /**
     * Getter for property 'taskClass'.
     *
     * @return Value for property 'taskClass'.
     */
    public Class<TTask> getTaskClass() {
        return taskClass;
    }

    /**
     * Getter for property 'resultClass'.
     *
     * @return Value for property 'resultClass'.
     */
    public Class<TResult> getResultClass() {
        return resultClass;
    }

    /**
     * Getter for property 'workerName'.
     *
     * @return Value for property 'workerName'.
     */
    public String getWorkerName() {
        return workerName;
    }

    /**
     * Getter for property 'apiVersion'.
     *
     * @return Value for property 'apiVersion'.
     */
    public int getApiVersion() {
        return apiVersion;
    }
}
