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
 * Represents a task to be completed by the CAF Worker.
 */
public interface WorkerTaskData
{
    /**
     * Retrieves an indicator of the type of the task
     */
    String getClassifier();

    /**
     * Retrieves the version of the task message used
     */
    int getVersion();

    /**
     * Retrieves the task status
     */
    TaskStatus getStatus();

    /**
     * Retrieves the actual task data in a serialised form
     */
    Object getData();

    /**
     * Retrieves any task specific context associated with the task
     */
    byte[] getContext();

    /**
     * Retrieves task specific correlationId
     */
    String getCorrelationId();

    /**
     * Retrieves tracking information associated with the task
     */
    TrackingInfo getTrackingInfo();

    /**
     * Retrieves the destination pipe to which the sender intends the task to be sent.
     */
    String getTo();

    /**
     * Retrieves information relating to the source of the task
     */
    TaskSourceInfo getSourceInfo();

    /**
     * Used when a task needs to return multiple responses. This method can be called to return responses prior to the final response.
     *
     * @param response the response to be sent
     * @param includeTaskContext whether or not the calling task context should be sent with the response
     */
    void addResponse(WorkerResponse response, boolean includeTaskContext);

    /**
     * Used when a task message needs to be published on the messaging queue.
     *
     * @param tm the task message to be sent
     */
    void sendMessage(TaskMessage tm);
}
