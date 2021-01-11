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

import java.util.EnumSet;
import java.util.Set;

/**
 * The state of a worker thread upon termination.
 */
public enum TaskStatus
{
    /**
     * This is a new task that requires work.
     */
    NEW_TASK,
    /**
     * The task data failed validation or could not be understood.
     */
    INVALID_TASK,
    /**
     * The worker succeeded at performing a task.
     */
    RESULT_SUCCESS,
    /**
     * The worker explicitly failed at a task.
     */
    RESULT_FAILURE,
    /**
     * The worker failed at a task because of an unhandled exception.
     */
    RESULT_EXCEPTION;

    /**
     * These are TaskStatus entries that qualify as results.
     */
    private static final Set<TaskStatus> STATUS_RESULTS = EnumSet.of(RESULT_SUCCESS, RESULT_FAILURE, RESULT_EXCEPTION);
    /**
     * These are TaskStatus entries that qualify as a successful response (but not necessarily a result).
     */
    private static final Set<TaskStatus> STATUS_RESPONSE_SUCCESS = EnumSet.of(RESULT_SUCCESS, NEW_TASK);

    /**
     * Determine if this TaskStatus classifies as a result
     *
     * @param status the TaskStatus to inspect
     * @return whether this TaskStatus classifies as a result
     */
    public static boolean isResult(final TaskStatus status)
    {
        return STATUS_RESULTS.contains(status);
    }

    /**
     * Determine if a TaskStatus classifies as a successful response
     *
     * @param status the TaskStatus to inspect
     * @return whether this TaskStatus classifies as a successful response
     */
    public static boolean isSuccessfulResponse(final TaskStatus status)
    {
        return STATUS_RESPONSE_SUCCESS.contains(status);
    }

}
