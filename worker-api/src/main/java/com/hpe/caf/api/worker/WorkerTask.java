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
package com.hpe.caf.api.worker;

/**
 * Provides access onto Worker Task Data and ability to set response.
 */
public interface WorkerTask extends WorkerTaskData
{
    /**
     * Used by the Worker to set the response to the task.
     */
    void setResponse(WorkerResponse response);

    /**
     * Used by the Worker to reject the task
     */
    void setResponse(TaskRejectedException taskRejectedException);

    /**
     * Used by the Worker to declare that the task is not valid
     */
    void setResponse(InvalidTaskException invalidTaskException);

    /**
     * Used by the Worker to determine that a message is poison and cannot be processed by the worker as it has failed and or crashed the
     * worker on number of occasions previously
     *
     * @return boolean if a message is poisoned
     */
    boolean isPoison();
}
