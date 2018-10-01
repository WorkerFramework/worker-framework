/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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

import java.io.OutputStream;
import java.util.function.Consumer;

/**
 * Supported by {@link DataStore} implementations which can supply an output stream for storing data.
 */
public interface DataStoreOutputStreamSupport
{
    /**
     * Opens an output stream which the caller can use to store data. The data will be stored relative to the partial reference supplied.
     * When the output stream is closed the supplied function will be invoked to return the reference which can subsequently be used to
     * retrieve the data.
     *
     * @param partialReference the partial reference, which the data will be stored relative to
     * @param setReferenceFunction the function which will be called to supply the reference for the data stored
     * @return an open {@link OutputStream} which can be used for storing data
     * @throws DataStoreException if the data store cannot service the request
     */
    OutputStream store(String partialReference, Consumer<String> setReferenceFunction) throws DataStoreException;
}
