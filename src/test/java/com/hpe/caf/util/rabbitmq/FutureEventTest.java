package com.hpe.caf.util.rabbitmq;


import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


public class FutureEventTest
{
    @Test
    public void testAsk()
        throws InterruptedException, ExecutionException, TimeoutException
    {
        FutureEvent<TestInterface, Boolean> t = new DummyFutureEvent();
        TestInterface it = Mockito.mock(TestInterface.class);
        Mockito.when(it.test()).thenReturn(true);
        t.handleEvent(it);
        Assert.assertEquals(true, t.ask().get(1, TimeUnit.SECONDS));
    }


    @Test(expected = TimeoutException.class)
    public void testAskTimeout()
        throws InterruptedException, ExecutionException, TimeoutException
    {
        FutureEvent<TestInterface, Boolean> t = new DummyFutureEvent();
        t.ask().get(1, TimeUnit.SECONDS);
    }


    public static class DummyFutureEvent extends FutureEvent<TestInterface, Boolean>
    {
        @Override
        public Boolean getEventResult(final TestInterface target)
        {
            return target.test();
        }
    }


    public interface TestInterface
    {
        boolean test();
    }
}
