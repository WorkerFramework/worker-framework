package com.hpe.caf.worker.testing;

import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskMessage;

import java.io.IOException;

/**
 * The interface for worker result processors.
 * Implementations of this interface can be used to process worker
 * results - messages created as a result of a worker execution.
 * Processing can include validation or saving of results.
 */
public interface ResultProcessor {

    /**
     * Process method is called when a worker under test produces a result.
     * {@link TestItem} used to create a source task for a worker is retrieved
     * from {@link TestItemStore} and passed along the result message.
     * This can be used to examine and validate the result.
     *
     * @param testItem      the test item
     * @param resultMessage the result message
     * @return the boolean
     * @throws CodecException the codec exception
     * @throws IOException    the io exception
     */
    boolean process(TestItem testItem, TaskMessage resultMessage) throws CodecException, IOException;

}
