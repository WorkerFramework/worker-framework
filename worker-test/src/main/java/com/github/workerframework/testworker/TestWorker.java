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

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskRejectedException;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.Worker;
import com.hpe.caf.api.worker.WorkerResponse;
import com.hpe.caf.api.worker.WorkerTaskData;
import jakarta.annotation.Nonnull;
import java.nio.charset.StandardCharsets;

final class TestWorker implements Worker
{
    private static final byte[] TEST_WORKER_RESULT = "TestWorkerResult".getBytes(StandardCharsets.UTF_8);

    private final TestWorkerConfiguration config;
    private final Codec codec;
    private final WorkerTaskData workerTask;

    public TestWorker(final TestWorkerConfiguration config, final Codec codec, final WorkerTaskData workerTask)
        throws InvalidTaskException, TaskRejectedException
    {
        this.config = config;
        this.codec = codec;
        this.workerTask = workerTask;
    }

    @Nonnull
    @Override
    public WorkerResponse doWork() throws InterruptedException, TaskRejectedException, InvalidTaskException {

        // Required for PoisonMessageIT. If isPoison is true, the worker will exit to simulate a poison message.
        final TestWorkerTask testWorkerTask;
        try {
            testWorkerTask = codec.deserialise(workerTask.getData(), TestWorkerTask.class);
            if(testWorkerTask.isPoison()){
                System.exit(1);
            }
        } catch (final CodecException e) {
            throw new RuntimeException(e);
        }

        final String outputQueue = config.getOutputQueue();

        if(testWorkerTask.getDelaySeconds() > 0) {
            // Used to test graceful shutdown in the ShutdownDeveloperTest
            Thread.sleep(testWorkerTask.getDelaySeconds() * 1000L);
        }
        
        return new WorkerResponse(
            outputQueue,
            TaskStatus.RESULT_SUCCESS,
            TEST_WORKER_RESULT,
            "TestWorkerResult",
            1,
            null);
    }

    @Nonnull
    @Override
    public WorkerResponse getGeneralFailureResult(final Throwable t)
    {
        final String outputQueue = config.getOutputQueue();

        return new WorkerResponse(
            outputQueue,
            TaskStatus.RESULT_SUCCESS,
            t.toString().getBytes(StandardCharsets.UTF_8),
            "TestWorkerFailureResult",
            1,
            null);
    }

    @Override
    public int getWorkerApiVersion()
    {
        return 1;
    }

    @Nonnull
    @Override
    public String getWorkerIdentifier()
    {
        return "TestWorker";
    }
}
