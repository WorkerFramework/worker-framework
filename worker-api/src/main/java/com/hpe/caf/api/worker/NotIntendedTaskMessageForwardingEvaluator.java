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

import com.hpe.caf.api.Codec;
import java.util.Map;

/**
 * A worker should implement this interface if it is capable of evaluating task messages that are not intended for it (because the
 * worker's input queue is different from the target/to queue in the task message), and deciding whether they are to be discarded,
 * executed or forwarded.
 */
public interface NotIntendedTaskMessageForwardingEvaluator
{
    /**
     * Examines the task message and decides whether to discard, execute or forward it.
     *
     * @param tm the task message
     * @param taskInformation the reference to the message this task arrived on
     * @param poison flag indicating if the message is a poison message
     * @param headers the map of key/value paired headers to be stamped on the message
     * @param codec the Codec that can be used to serialise/deserialise data
     * @param jobStatus the job status as returned by the status check URL
     * @param callback worker callback to enact the forwarding action determined by the worker
     * @return the forwarding action to be taken by the worker
     */
    TaskForwardingAction determineForwardingAction(
        TaskMessage tm,
        TaskInformation taskInformation,
        boolean poison,
        Map<String, Object> headers,
        Codec codec,
        JobStatus jobStatus,
        WorkerCallback callback);
}
