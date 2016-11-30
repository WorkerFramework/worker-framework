/*
 * (c) Copyright 2015-2016 Hewlett Packard Enterprise Development LP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import com.hpe.caf.worker.testing.TestConfiguration;
import com.hpe.caf.worker.testing.TestItem;
import com.hpe.caf.worker.testing.preparation.PreparationItemProvider;

import java.nio.file.Path;

/**
 * Result preparation provider for preparing test items.
 * Generates Test items from the yaml serialised test case files.
 */
public class ${workerName}ResultPreparationProvider  extends PreparationItemProvider<${workerName}Task, ${workerName}Result, ${workerName}TestInput, ${workerName}TestExpectation> {

    public ${workerName}ResultPreparationProvider(TestConfiguration<${workerName}Task, ${workerName}Result, ${workerName}TestInput, ${workerName}TestExpectation> configuration) {
        super(configuration);
    }

    /**
     * Method for generating test items from the yaml testcases.
     * Creates ${workerName}TestInput and ${workerName}TestExpectation objects (which contain ${workerName}Task and ${workerName}Result).
     * The ${workerName}Task found in ${workerName}TestInput is fed into the worker for the integration test, and the result is
     * compared with the ${workerName}Result found in the ${workerName}TestExpectation.
     * @param inputFile
     * @param expectedFile
     * @return TestItem
     * @throws Exception
     */
    @Override
    protected TestItem createTestItem(Path inputFile, Path expectedFile) throws Exception {
        TestItem<${workerName}TestInput, ${workerName}TestExpectation> item = super.createTestItem(inputFile, expectedFile);
        ${workerName}Task task = getTaskTemplate();

        // if the task is null, put in default values
        if(task==null){
            task=new ${workerName}Task();
            task.action = ${workerName}Action.VERBATIM;
        }

        item.getInputData().setTask(task);
        return item;
    }
}
