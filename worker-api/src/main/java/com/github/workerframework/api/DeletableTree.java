/*
 * Copyright 2015-2025 Open Text.
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
package com.github.workerframework.api;

/**
 * An interface intended to be implemented by DataStores that support the concept of a unique filepath to each stored asset.
 */
public interface DeletableTree
{
    /**
     * Delete asset identified by reference
     *
     * @param reference a complete reference to be interpreted by the DataStore implementation
     * @throws DataStoreException if data store cannot service the request
     */
    void deleteTree(final String reference) throws DataStoreException;
}
