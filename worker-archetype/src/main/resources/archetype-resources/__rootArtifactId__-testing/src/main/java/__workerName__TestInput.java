#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import com.hpe.caf.worker.testing.FileTestInputData;

/**
 * ${workerName}TestInput is a component of test item, and contains a worker task used to provide test work to a worker.
 */
public class ${workerName}TestInput extends FileTestInputData {

    /**
     * ${workerName}Task read in from the yaml test case and used as an input of test work to the worker.
     */
    private ${workerName}Task task;

    public ${workerName}TestInput() {
    }

    public ${workerName}Task getTask() {
        return task;
    }

    public void setTask(${workerName}Task task) {
        this.task = task;
    }
}
