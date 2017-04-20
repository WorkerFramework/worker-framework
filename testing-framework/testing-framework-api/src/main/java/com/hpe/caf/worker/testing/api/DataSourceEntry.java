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

/**
 * Created by ploch on 14/04/2017.
 */
public class DataSourceEntry<TData>
{
    private final TData data;
    private final String id;

    public DataSourceEntry(TData data, String id)
    {
        this.data = data;
        this.id = id;
    }

    /**
     * Getter for property 'data'.
     *
     * @return Value for property 'data'.
     */
    public TData getData()
    {
        return data;
    }

    /**
     * Getter for property 'id'.
     *
     * @return Value for property 'id'.
     */
    public String getId()
    {
        return id;
    }
}
