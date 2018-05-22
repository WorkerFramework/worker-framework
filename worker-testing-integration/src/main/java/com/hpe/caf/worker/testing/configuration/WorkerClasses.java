/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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
package com.hpe.caf.worker.testing.configuration;

/**
 * Created by ploch on 04/12/2015.
 */
public class WorkerClasses<TWorkerTask, TWorkerResult>
{
    private Class<TWorkerTask> workerTaskClass;

    private Class<TWorkerResult> workerResultClass;

    public WorkerClasses(Class<TWorkerTask> workerTaskClass, Class<TWorkerResult> workerResultClass)
    {
        this.workerTaskClass = workerTaskClass;
        this.workerResultClass = workerResultClass;
    }

    /**
     * Getter for property 'workerTaskClass'.
     *
     * @return Value for property 'workerTaskClass'.
     */
    public Class<TWorkerTask> getWorkerTaskClass()
    {
        return workerTaskClass;
    }

    /**
     * Getter for property 'workerResultClass'.
     *
     * @return Value for property 'workerResultClass'.
     */
    public Class<TWorkerResult> getWorkerResultClass()
    {
        return workerResultClass;
    }
}
