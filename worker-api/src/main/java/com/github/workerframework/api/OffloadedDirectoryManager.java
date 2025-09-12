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

import java.nio.file.Path;

/**
 * An interface intended to be implemented by FileSystemDataStores that support the concept of a
 * unique filepath to each stored asset.
 */
public interface OffloadedDirectoryManager
{
    /**
     * Delete an offloaded assert and its containing directory tree.
     *
     * @param path a path to be interpreted by the DataStore implementation
     * @throws DataStoreException if data store cannot service the request
     */
    void deleteOffloadingTree(final Path path)
        throws DataStoreException;
}
