package com.hpe.caf.worker.core;

import com.hpe.caf.api.worker.TaskRejectedException;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class WorkerThreadPoolTest
{
    @Test
    public void testWorkerThreadPool()
        throws TaskRejectedException, InterruptedException
    {
        CountDownLatch latch = new CountDownLatch(1);
        WorkerThreadPool wtp = new WorkerThreadPool(2, latch::countDown);
        wtp.submit(new TestCrash(), "test-id");
        latch.await(1, TimeUnit.SECONDS);
    }

    private static class TestCrash implements Runnable
    {
        @Override
        public void run() {
            throw new Error("whoops!");
        }
    }
}
