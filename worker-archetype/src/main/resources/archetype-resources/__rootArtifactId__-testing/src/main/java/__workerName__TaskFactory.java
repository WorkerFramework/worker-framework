#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.testing.FileInputWorkerTaskFactory;
import com.hpe.caf.worker.testing.TestConfiguration;
import com.hpe.caf.worker.testing.TestItem;

/**
 * Task factory for creating tasks from test item.
 */
public class ${workerName}TaskFactory extends FileInputWorkerTaskFactory<${workerName}Task, ${workerName}TestInput, ${workerName}TestExpectation> {
    public ${workerName}TaskFactory(TestConfiguration configuration) throws Exception {
        super(configuration);
    }

    /**
     * Creates a task from a test item (the test item is generated from the yaml test case).
     * @param testItem
     * @param sourceData
     * @return ${workerName}Task
     */
    @Override
    protected ${workerName}Task createTask(TestItem<${workerName}TestInput, ${workerName}TestExpectation> testItem, ReferencedData sourceData) {
        ${workerName}Task task = testItem.getInputData().getTask();

        //setting task source data to the source data parameter.
        task.sourceData = sourceData;
        task.datastorePartialReference = getContainerId();
        task.action = testItem.getInputData().getTask().action;

        return task;
    }

    @Override
    public String getWorkerName() {
        return ${workerName}Constants.WORKER_NAME;
    }

    @Override
    public int getApiVersion() {
        return ${workerName}Constants.WORKER_API_VER;
    }
}
