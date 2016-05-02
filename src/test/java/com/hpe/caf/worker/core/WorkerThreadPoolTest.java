package com.hpe.caf.worker.core;

import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskRejectedException;
import com.hpe.caf.api.worker.Worker;
import org.junit.Test;

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
    }
}
