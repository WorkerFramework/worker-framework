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
package com.hpe.caf.worker.testing.api;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * <p>
 *     Represents any variable test data, potentially a file, collection of files or anything else.
 * </p>
 * <p>
 *     During recording of a test, an implementation of this type will hold information about
 *     any data that can be used to populate a task.
 * </p>
 * <p>
 *     For example, if a worker operates on a binary file, test recording mode will scan a supplied
 *     directory and create {@code TestDataSource} for each file. Framework will map it to appropriate
 *     place in a worker message.
 * </p>
 */
public class TestDataSource
{
    private final Set<DataSourceEntry> entries = new HashSet<>();

    public void addData(Object data)
    {
        entries.add(new DataSourceEntry(data, null));
    }

    public void addData(Object data, String id)
    {
        entries.add(new DataSourceEntry(data, id));
    }

    public Stream<DataSourceEntry> stream()
    {
        return entries.stream();
    }

    public <TData> TData getData(Class<TData> dataClass, String id)
    {
        Optional<DataSourceEntry> sourceEntry = entries.stream().filter(entry -> dataClass.isAssignableFrom(entry.getData().getClass()) && entry.getId().equals(id)).findFirst();
        if (!sourceEntry.isPresent()) {
            return null;
        }
        return (TData) sourceEntry.get().getData();
    }

    public <TData> TData getData(Class<TData> dataClass)
    {
        Optional<DataSourceEntry> sourceEntry = entries.stream().filter(entry -> dataClass.isAssignableFrom(entry.getData().getClass())).findFirst();
        if (!sourceEntry.isPresent()) {
            return null;
        }
        return (TData) sourceEntry.get().getData();
    }
}
