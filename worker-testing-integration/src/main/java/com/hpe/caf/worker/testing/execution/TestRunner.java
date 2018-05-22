/*
 * Copyright 2018-2017 EntIT Software LLC, a Micro Focus company.
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
package com.hpe.caf.worker.testing.execution;

import com.hpe.caf.worker.testing.TestController;

/**
 * Created by ploch on 17/12/2015.
 */
public class TestRunner
{
    private TestRunner()
    {
    }

    public static void runTests(TestControllerProvider controllerProvider) throws Exception
    {
        runTests(controllerProvider, false);
    }

    public static void runTests(TestControllerProvider controllerProvider, boolean dataGenerationMode) throws Exception
    {

        run(dataGenerationMode ? controllerProvider.getDataPreparationController() : controllerProvider.getTestController());
    }

    public static void runTests(Class<TestControllerProvider> controllerProviderClass, boolean dataGenerationMode) throws Exception
    {
        TestControllerProvider controllerProvider = controllerProviderClass.newInstance();
        runTests(controllerProvider, dataGenerationMode);
    }

    public static void run(TestController controller) throws Exception
    {
        try {
            controller.runTests();
        } finally {
            controller.close();
        }
    }
}
