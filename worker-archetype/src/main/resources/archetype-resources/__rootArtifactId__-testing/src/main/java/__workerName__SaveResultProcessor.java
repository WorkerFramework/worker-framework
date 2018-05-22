/*
 * Copyright 2018-2017 EntIT Software LLC, a Micro Focus company.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
