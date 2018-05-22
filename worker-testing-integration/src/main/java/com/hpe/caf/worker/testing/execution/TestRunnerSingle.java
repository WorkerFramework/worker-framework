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
package com.hpe.caf.worker.testing.execution;

import com.hpe.caf.worker.testing.TestControllerSingle;
import com.hpe.caf.worker.testing.TestItem;
import com.hpe.caf.worker.testing.TestItemProvider;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by oloughli on 27/05/2016.
 */
public class TestRunnerSingle
{
    private static TestItemProvider itemProvider;
    private static boolean mode;

    public static Set<Object[]> setUpTest(TestControllerProvider controllerProvider) throws Exception
    {
        try {
            Collection<TestItem> items = getItemProvider(controllerProvider, mode).getItems();
            Set<Object[]> s = new HashSet<>();
            for (TestItem i : items) {
                s.add(new Object[]{i, i.getTag()});

                if (i.getInputIdentifier() == null) {
                    i.setInputIdentifier(i.getTag());
                }
            }
            return s;
        } catch (Throwable e) {
            System.out.println("Exception happened during testcase loading: " + e.toString());
            e.printStackTrace();
            throw e;
        }
    }

    public static TestControllerSingle getTestController(TestControllerProvider controllerProvider, boolean dataGenerationMode) throws Exception
    {
        mode = dataGenerationMode;
        TestControllerSingle controller = dataGenerationMode ? controllerProvider.getNewDataPreparationController() : controllerProvider.getNewTestController();
        return controller;
    }

    public static TestItemProvider getItemProvider(TestControllerProvider controllerProvider, boolean typeOfItemProvider)
    {
        itemProvider = controllerProvider.getItemProvider(typeOfItemProvider);
        return itemProvider;
    }

    public static TestItemProvider getItemProvider()
    {
        return itemProvider;
    }
}
