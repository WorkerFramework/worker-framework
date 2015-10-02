package com.hpe.caf.worker.core;


import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class WorkerThreadPoolExecutorTest
{
    @Test
    public void testWorkerThreadPoolExecutor()
        throws InterruptedException
    {
        CountDownLatch latch = new CountDownLatch(1);
        ThreadPoolExecutor tpe = new WorkerThreadPoolExecutor(2, 2, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(), latch::countDown);
        tpe.submit(new TestCrash());
        latch.await(1, TimeUnit.SECONDS);
    }


    private static class TestCrash implements Runnable
    {
        @Override
        public void run()
        {
            throw new Error("whoops!");
        }
    }
}
