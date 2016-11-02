#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import com.hpe.caf.worker.testing.ContentFileTestExpectation;

/**
 * ${workerName}TestExpectation forms a component of the test item, and contains the expected ${workerName}Result, used to compare
 * with the actual worker result.
 */
public class ${workerName}TestExpectation  extends ContentFileTestExpectation {

    /**
     * ${workerName}Result read in from the yaml test case, used to validate the result of the worker is as expected.
     */
    private ${workerName}Result result;

    public ${workerName}TestExpectation() {
    }

    public ${workerName}Result getResult() {
        return result;
    }

    public void setResult(${workerName}Result result) {
        this.result = result;
    }
}
