package com.hpe.caf.worker.testing;

import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.util.rabbitmq.Delivery;

import java.io.IOException;

/**
 * Created by ploch on 08/11/2015.
 */
public interface ResultHandler {

    void handleResult(TaskMessage taskMessage) throws IOException, CodecException;

}
