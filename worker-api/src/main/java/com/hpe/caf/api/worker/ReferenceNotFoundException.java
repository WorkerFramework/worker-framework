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

/**
 * Indicates the reference passed to the DataStore did not point to any resolvable location.
 */
public class ReferenceNotFoundException extends DataStoreException
{
    public ReferenceNotFoundException(final String message)
    {
        super(message);
    }

    public ReferenceNotFoundException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}
