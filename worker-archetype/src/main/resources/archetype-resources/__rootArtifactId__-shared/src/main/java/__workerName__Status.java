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

/**
 * Enumeration representing the status of the worker result.
 */
public enum ${workerName}Status {

    /**
     * Worker processed task and was successful.
     */
    COMPLETED,

    /**
     * The source data could not be acquired from the DataStore.
     */
    SOURCE_FAILED,

    /**
     * The result could not be stored in the DataStore.
     */
    STORE_FAILED,

    /**
     * The input file could be read but the worker failed in an unexpected way.
     */
    WORKER_FAILED
}
