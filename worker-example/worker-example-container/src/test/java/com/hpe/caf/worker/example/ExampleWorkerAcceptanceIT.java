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
package com.hpe.caf.worker.example;

import com.hpe.caf.worker.testing.TestControllerSingle;
import com.hpe.caf.worker.testing.TestItem;
import com.hpe.caf.worker.testing.UseAsTestName;
import com.hpe.caf.worker.testing.UseAsTestName_TestBase;
import com.hpe.caf.worker.testing.execution.TestControllerProvider;
import com.hpe.caf.worker.testing.execution.TestRunnerSingle;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Iterator;
import java.util.Set;

/**
 * Integration test for Example worker, running the testing framework.
 */
public class ExampleWorkerAcceptanceIT extends UseAsTestName_TestBase
{
    TestControllerProvider testControllerProvider;
    TestControllerSingle controller;

    @BeforeClass
    public void setUp() throws Exception
    {
        testControllerProvider = new ExampleTestControllerProvider();
        controller = TestRunnerSingle.getTestController(testControllerProvider, false);
        controller.initialise();
    }

    @AfterClass
    public void tearDown() throws Exception
    {
        controller.close();
    }

    @DataProvider(name = "MainTest")
    public Iterator<Object[]> createData() throws Exception
    {
        Set<Object[]> s = TestRunnerSingle.setUpTest(testControllerProvider);
        return s.iterator();
    }

    @UseAsTestName(idx = 1)
    @Test(dataProvider = "MainTest")
    public void testWorker(TestItem testItem, String testName) throws Exception
    {
        controller.runTests(testItem);
    }
}
