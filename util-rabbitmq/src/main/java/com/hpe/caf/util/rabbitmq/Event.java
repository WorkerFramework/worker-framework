/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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
package com.hpe.caf.util.rabbitmq;

/**
 * A general event trigger with a target.
 *
 * @param <T> the class or interface of the target the Event applies to
 */
@FunctionalInterface
public interface Event<T>
{
    /**
     * Trigger the action represented by this Event.
     *
     * @param target the class to perform an action on
     */
    void handleEvent(final T target);
}
