/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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
package com.hpe.caf.worker.testing.api;

import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskMessage;

import java.io.IOException;

/**
 * The interface for worker result processors.
 * Implementations of this interface can be used to process worker
 * results - messages created as a result of a worker execution.
 * Processing can include validation or saving of results.
 */
public interface TaskMessageHandler {

    /**
     * Process method is called when a worker under test produces a result.
     * {@link TestItem} Contains data required to construct a worker task as well as expected result for validation.
     *
     * @param resultMessage the result message
     */
    void handle(TaskMessage resultMessage);

}
