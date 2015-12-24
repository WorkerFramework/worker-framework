package com.hpe.caf.worker.testing.execution;

import com.hpe.caf.worker.testing.TestController;

/**
 * Created by ploch on 17/12/2015.
 */
public interface TestControllerProvider {

    TestController getTestController() throws Exception;

    TestController getDataPreparationController()  throws Exception;
}
