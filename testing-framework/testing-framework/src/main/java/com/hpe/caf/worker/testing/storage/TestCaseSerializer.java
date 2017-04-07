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
package com.hpe.caf.worker.testing.storage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * Created by ploch on 16/03/2017.
 */
public abstract class TestCaseSerializer  {

    private final ObjectMapper mapper;

    protected TestCaseSerializer() {
        this.mapper = createConfiguredMapper();
    }

    protected abstract ObjectMapper createConfiguredMapper();

    public byte[] serialise(Object object) throws JsonProcessingException {
        return mapper.writeValueAsBytes(object);
    }

    public <T> T deserialise(byte[] data, Class<T> clazz) throws IOException {

        return mapper.readValue(data, clazz);
    }
}
