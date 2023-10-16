/*
 * Copyright 2015-2023 Open Text.
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

import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskRejectedException;
import com.hpe.caf.api.worker.TaskStatus;
import com.hpe.caf.api.worker.Worker;
import com.hpe.caf.api.worker.WorkerResponse;
import com.hpe.caf.api.worker.WorkerTaskData;
import java.nio.charset.StandardCharsets;

final class TestWorker implements Worker
{
    private static final byte[] TEST_WORKER_RESULT = "TestWorkerResult".getBytes(StandardCharsets.UTF_8);

    private final TestWorkerConfiguration config;

    public TestWorker(final TestWorkerConfiguration config, final WorkerTaskData workerTask)
        throws InvalidTaskException, TaskRejectedException
    {
        this.config = config;
    }

    @Override
    public WorkerResponse doWork() throws InterruptedException, TaskRejectedException, InvalidTaskException
    {
        final String outputQueue = config.getOutputQueue();
        final String workerName = config.getWorkerName();
//        throw new TaskRejectedException("rejected...poison message");

//        throw new RuntimeException("poison");

//        System.exit(1);

        return new WorkerResponse(
            outputQueue,
            TaskStatus.RESULT_SUCCESS,
            TEST_WORKER_RESULT,
            "TestWorkerResult",
            1,
            null);
    }

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

    @Override
    public String getWorkerIdentifier()
    {
        return "TestWorker";
    }

    @Override
    public String getWorkerName(){
        return config.getWorkerName();
    }

}
