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
package com.opentext.caf.api.worker;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.ConfigurationSource;

/**
 * The responsibility of a WorkerFactory is to provide a mechanism to generate new Worker objects and specify how many simultaneous
 * workers should be running.
 */
public interface WorkerFactoryProvider
{
    /**
     * Generate a new worker given task data.
     *
     * @param configSource the configuration source optionally used to configure the workers
     * @param dataStore a datastore which is optionally available to workers
     * @param codec the Codec that can be used to serialise/deserialise data
     * @return a new worker
     * @throws WorkerException if a new Worker cannot be generated
     */
    WorkerFactory getWorkerFactory(ConfigurationSource configSource, DataStore dataStore, Codec codec)
        throws WorkerException;
}
