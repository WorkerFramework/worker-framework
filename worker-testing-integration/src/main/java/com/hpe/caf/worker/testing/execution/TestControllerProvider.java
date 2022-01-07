/*
 * Copyright 2015-2021 Micro Focus or one of its affiliates.
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

/**
 * The test controller provider interface. Implementations are responsible for creating configured {@link TestController} instances used
 * to execute worker tests or preparation of initial test case data. Implementations of this interface should be advertised to
 * {@link com.hpe.caf.util.ModuleLoader} and {@link java.util.ServiceLoader} using resource configuration file (META-INF/services).
 */
public interface TestControllerProvider
{
    /**
     * Gets the name of a worker under test. When worker test application is invoked this name will be used to identify which worker
     * should be tested.
     *
     * @return the name of a worker
     */
    String getWorkerName();

    /**
     * Gets test controller.
     *
     * @return the test controller
     * @throws Exception
     */
    TestController getTestController() throws Exception;

    /**
     * Gets initial test case data preparation controller. This controller will be used when tests are running in the data generation
     * mode.
     *
     * @return the data preparation controller
     * @throws Exception
     */
    TestController getDataPreparationController() throws Exception;

    /*
    * Gets the configuration for the item provider. This can be used in the setup for the Test Classes
    * @return the TestConfiguration
     */
    TestItemProvider getItemProvider(boolean typeOfItemProvider);

    /*
    * Gets the controller
    *
    * @return the data preparation controller
    * @throws Exception
    * */
    TestControllerSingle getNewDataPreparationController() throws Exception;

    /*
    * Gets the controller
    *
    * @return the test controller
    * @throws Exception
    * */
    TestControllerSingle getNewTestController() throws Exception;
}
