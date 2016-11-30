/*
 * (c) Copyright 2015-2016 Hewlett Packard Enterprise Development LP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
        throws Exception
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


    @Test(expected = ExecutionException.class)
    public void testException()
        throws Exception
    {
        FutureEvent<TestInterface, Boolean> t = new DummyFutureEvent();
        TestInterface it = Mockito.mock(TestInterface.class);
        Mockito.when(it.test()).thenThrow(Exception.class);
        t.handleEvent(it);
        t.ask().get(1, TimeUnit.SECONDS);
    }


    public static class DummyFutureEvent extends FutureEvent<TestInterface, Boolean>
    {
        @Override
        public Boolean getEventResult(final TestInterface target)
            throws Exception
        {
            return target.test();
        }
    }


    public interface TestInterface
    {
        boolean test()
            throws Exception;
    }
}
