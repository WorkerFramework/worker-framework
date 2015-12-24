package com.hpe.caf.worker.testing.execution;

import com.hpe.caf.worker.testing.TestController;

/**
 * Created by ploch on 17/12/2015.
 */
public class TestRunner {

    private TestRunner(){}

    public static void runTests(TestControllerProvider controllerProvider) throws Exception {
        runTests(controllerProvider, false);
    }

    public static void runTests(TestControllerProvider controllerProvider, boolean dataGenerationMode) throws Exception {

        run(dataGenerationMode ? controllerProvider.getDataPreparationController() : controllerProvider.getTestController());
    }

    public static void runTests(Class<TestControllerProvider> controllerProviderClass, boolean dataGenerationMode) throws Exception {
        TestControllerProvider controllerProvider = controllerProviderClass.newInstance();
        runTests(controllerProvider, dataGenerationMode);
    }


    public static void run(TestController controller) throws Exception {
        try {
            controller.runTests();
        }
        finally {
            controller.close();
        }
    }


}
