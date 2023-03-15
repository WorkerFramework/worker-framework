/*
 * Copyright 2015-2023 Open Text.
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

import java.util.Map;

/**
 * A worker should implement this interface if it is capable of evaluating task messages and deciding whether they are to be forwarded
 * rather than executed.
 *
 * @deprecated This interface is limited in the arguments the determineForwardingAction method takes, and the fact that it can only
 * return void. Use {@link DivertedTaskHandler} instead.
 */
@Deprecated
public interface TaskMessageForwardingEvaluator
{
    /**
     * Examines the task message and decides whether to forward it or take some other action, e.g. discard.
     *
     * @param tm the task message
     * @param taskInformation the reference to the message this task arrived on
     * @param headers the map of key/value paired headers to be stamped on the message
     * @param callback worker callback to enact the forwarding action determined by the worker
     */
    void determineForwardingAction(TaskMessage tm, TaskInformation taskInformation, Map<String, Object> headers, WorkerCallback callback);
}
