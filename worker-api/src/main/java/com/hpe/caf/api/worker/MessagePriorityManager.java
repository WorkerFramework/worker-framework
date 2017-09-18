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
package com.hpe.caf.api.worker;

/**
 * An interface for setting up message priorities as they progress.
 */
public interface MessagePriorityManager
{
    /**
     * Gets the priority of response message.
     *
     * @param originalTaskMessage A worker input (request) message.
     * @return Priority
     */
    Integer getResponsePriority(final TaskMessage originalTaskMessage);
}
