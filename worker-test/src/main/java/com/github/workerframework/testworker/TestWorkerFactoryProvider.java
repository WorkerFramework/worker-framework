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
package com.github.workerframework.testworker;

import com.github.cafapi.common.api.Codec;
import com.github.cafapi.common.api.ConfigurationSource;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.WorkerException;
import com.hpe.caf.api.worker.WorkerFactory;
import com.hpe.caf.api.worker.WorkerFactoryProvider;
import jakarta.annotation.Nonnull;

/**
 * Simple test worker used for testing the Worker Framework.
 */
public final class TestWorkerFactoryProvider implements WorkerFactoryProvider
{
    @Nonnull
    @Override
    public WorkerFactory getWorkerFactory(
        final ConfigurationSource configSource,
        final DataStore dataStore,
        final Codec codec
    ) throws WorkerException
    {
        return new TestWorkerFactory(configSource, dataStore, codec);
    }
}
