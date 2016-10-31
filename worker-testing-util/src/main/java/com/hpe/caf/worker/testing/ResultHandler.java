package com.hpe.caf.worker.testing;

import com.hpe.caf.api.worker.TaskMessage;

/**
 * Created by ploch on 08/11/2015.
 */
public interface ResultHandler {

    void handleResult(TaskMessage taskMessage);

}
