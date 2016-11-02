#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.worker.testing.TestConfiguration;
import com.hpe.caf.worker.testing.TestItem;
import com.hpe.caf.worker.testing.WorkerServices;
import com.hpe.caf.worker.testing.preparation.PreparationResultProcessor;

public class ${workerName}SaveResultProcessor extends PreparationResultProcessor<${workerName}Task, ${workerName}Result, ${workerName}TestInput, ${workerName}TestExpectation>
{
    public ${workerName}SaveResultProcessor(TestConfiguration<${workerName}Task, ${workerName}Result, ${workerName}TestInput, ${workerName}TestExpectation> configuration, WorkerServices workerServices)
    {

        super(configuration, workerServices.getCodec());
    }

    @Override
    protected byte[] getOutputContent(${workerName}Result workerResult, TaskMessage message, TestItem<${workerName}TestInput, ${workerName}TestExpectation> testItem)
            throws Exception
    {
        testItem.getExpectedOutputData().setResult(workerResult);
        return super.getOutputContent(workerResult, message, testItem);
    }
}
