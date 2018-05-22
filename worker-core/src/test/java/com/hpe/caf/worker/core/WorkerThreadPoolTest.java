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
package com.hpe.caf.worker.core;

import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskRejectedException;
import com.hpe.caf.api.worker.Worker;
import org.junit.Assert;
import org.testng.annotations.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.mockito.Mockito;

public class WorkerThreadPoolTest
{
    @Test
    public void testWorkerThreadPool()
        throws TaskRejectedException, InterruptedException, InvalidTaskException
    {
        CountDownLatch latch = new CountDownLatch(1);
        WorkerThreadPool wtp = WorkerThreadPool.create(2, latch::countDown);

        Worker mockWorker = Mockito.mock(Worker.class);
        Mockito.when(mockWorker.doWork()).thenThrow(new Error("whoops!"));

        WorkerTaskImpl mockWorkerTask = Mockito.mock(WorkerTaskImpl.class);
        Mockito.when(mockWorkerTask.createWorker()).thenReturn(mockWorker);

        wtp.submitWorkerTask(mockWorkerTask);
        latch.await(1, TimeUnit.SECONDS);
        Assert.assertEquals("Latch count should be 0; indicating that the handler was invoked due to the Error thrown",
                            0, latch.getCount());
    }
}
