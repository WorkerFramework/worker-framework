/*
 * Copyright 2015-2021 Micro Focus or one of its affiliates.
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
package com.hpe.caf.worker.testing;

import com.hpe.caf.api.worker.QueueTaskMessage;
import com.hpe.caf.api.worker.TaskMessage;

/**
 * Created by ploch on 08/11/2015.
 */
public class CompositeResultsProcessor implements ResultProcessor
{
    private final ResultProcessor[] processors;

    public CompositeResultsProcessor(ResultProcessor... processors)
    {

        this.processors = processors;
    }

    @Override
    public boolean process(TestItem testItem, TaskMessage resultMessage) throws Exception
    {
        boolean success = true;
        for (ResultProcessor processor : processors) {
            if (!processor.process(testItem, resultMessage)) {
                success = false;
            }
        }
        return success;
    }

    @Override
    public boolean process(TestItem testItem, QueueTaskMessage resultMessage) throws Exception
    {
        boolean success = true;
        for (ResultProcessor processor : processors) {
            if (!processor.process(testItem, resultMessage)) {
                success = false;
            }
        }
        return success;
    }

    public String getInputIdentifier(TaskMessage message)
    {
        return "";
    }
    public String getInputIdentifier(QueueTaskMessage message)
    {
        return "";
    }
}
