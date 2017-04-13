/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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
package com.hpe.caf.worker.testing.util;

import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CodeConfigurationSourceTest
{

    @Test
    public void testRetrievesConfigurationSuppliedInConstructor() throws Exception
    {

        TestSource1 source1 = new TestSource1();
        TestSource2 source2 = new TestSource2();

        CodeConfigurationSource sut = new CodeConfigurationSource(source1, source2);

        TestSource1 actualSource1 = sut.getConfiguration(TestSource1.class);
        TestSource2 actualSource2 = sut.getConfiguration(TestSource2.class);

        assertThat(actualSource1, is(source1));
        assertThat(actualSource2, is(source2));

    }

    @Test
    public void testRetrievesAddedConfiguration() throws Exception
    {
        TestSource1 source1 = new TestSource1();
        TestSource2 source2 = new TestSource2();

        CodeConfigurationSource sut = new CodeConfigurationSource(source1);
        sut.addConfiguration(source2);

        TestSource1 actualSource1 = sut.getConfiguration(TestSource1.class);
        TestSource2 actualSource2 = sut.getConfiguration(TestSource2.class);

        assertThat(actualSource1, is(source1));
        assertThat(actualSource2, is(source2));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testThrowsWhenConfigurationExists() throws Exception
    {
        TestSource1 source1 = new TestSource1();
        TestSource2 source2 = new TestSource2();
        TestSource2 anotherSource2 = new TestSource2();

        CodeConfigurationSource sut = new CodeConfigurationSource(source1, source2);

        sut.addConfiguration(anotherSource2);
    }

    @Test
    public void testCanOverrideConfiguration() throws Exception
    {
        TestSource1 source1 = new TestSource1();
        TestSource2 source2 = new TestSource2();
        TestSource2 anotherSource2 = new TestSource2();

        CodeConfigurationSource sut = new CodeConfigurationSource(source1, source2);
        sut.addConfiguration(anotherSource2, true);

        TestSource1 actualSource1 = sut.getConfiguration(TestSource1.class);
        TestSource2 actualSource2 = sut.getConfiguration(TestSource2.class);

        assertThat(actualSource1, is(source1));
        assertThat(actualSource2, is(anotherSource2));
    }

    class TestSource1
    {
    }

    class TestSource2
    {
    }

    class TestSource3
    {
    }
}
