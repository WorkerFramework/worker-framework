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
public class TestRunnerSingle {
    private static TestItemProvider itemProvider;

    public static Set<Object[]> setUpTest(TestControllerProvider controllerProvider, boolean typeOfItemProvider) throws Exception{
        Collection<TestItem> items = getItemProvider(controllerProvider,typeOfItemProvider).getItems();
        Set<Object[]> s=new HashSet<Object[]>();
        for (TestItem i: items) {
            s.add(new Object[]{i});
        }
        return s;
    }

    public static TestControllerSingle getTestController(TestControllerProvider controllerProvider, boolean dataGenerationMode) throws Exception {

        TestControllerSingle controller = dataGenerationMode? controllerProvider.getNewDataPreparationController() : controllerProvider.getNewTestController();
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
