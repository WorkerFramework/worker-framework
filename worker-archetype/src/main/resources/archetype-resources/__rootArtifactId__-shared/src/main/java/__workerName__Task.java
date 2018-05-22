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
#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import com.hpe.caf.util.ref.ReferencedData;

import javax.validation.constraints.NotNull;

/**
 * The task supplied to the worker. This is the main means of communication to the worker, providing the ReferencedData and
 * the action to take.
 */
public final class ${workerName}Task {

    /**
     * The ReferencedData file in the DataStore.
     */
    @NotNull
    public ReferencedData sourceData;

    /**
     * Identifies a target (relative) location for any output data that worker will save in data store.
     * If datastore-cs is used, this value will identify a target data store container to use.
     * If a worker needs to store output data, this should be used when calling {@link com.hpe.caf.api.worker.DataStore${symbol_pound}store(Path, String)} method.
     * Example usage is located in ${workerName} class, 'wrapAsReferencedData' method.
     */
    public String datastorePartialReference;

    /**
     * Enumeration to represent which action the worker will perform.
     */
    public ${workerName}Action action;
}
