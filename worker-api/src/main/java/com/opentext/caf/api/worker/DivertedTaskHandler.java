/*
 * Copyright 2015-2024 Open Text.
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
package com.opentext.caf.api.worker;

import com.hpe.caf.api.Codec;
import java.util.Map;

/**
 * A worker should implement this interface if it is capable of evaluating task messages that have been diverted (maybe because the
 * worker's input queue is different from the target/to queue in the task message), and deciding what to do with them.
 */
public interface DivertedTaskHandler
{
    /**
     * Examines the task message and decides what to do with it.
     *
     * @param tm the task message
     * @param taskInformation the reference to the message this task arrived on
     * @param headers the map of key/value paired headers to be stamped on the message
     * @param codec the Codec that can be used to serialise/deserialise data
     * @param jobStatus the job status as returned by the status check URL
     * @param callback worker callback to enact the diverted action determined by the worker
     * @return the diverted task action to be taken by the worker
     */
    DivertedTaskAction handleDivertedTask(
        TaskMessage tm,
        TaskInformation taskInformation,
        Map<String, Object> headers,
        Codec codec,
        JobStatus jobStatus,
        WorkerCallback callback);
}
