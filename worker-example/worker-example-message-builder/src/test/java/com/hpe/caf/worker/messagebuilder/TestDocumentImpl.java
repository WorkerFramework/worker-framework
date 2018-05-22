/*
 * Copyright 2018-2017 EntIT Software LLC, a Micro Focus company.
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
package com.hpe.caf.worker.messagebuilder;

import com.hpe.caf.messagebuilder.Document;

/**
 * POJO implementation of Document interface for use with testing ExampleWorkerMessageBuilder
 */
public class TestDocumentImpl implements Document
{
    private String storageReference;

    public TestDocumentImpl(String storageReference)
    {
        this.storageReference = storageReference;
    }

    public String getStorageReference()
    {
        return this.storageReference;
    }
}
