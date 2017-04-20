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
package com.hpe.caf.worker.testing.prototypes;

import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.testing.api.InputFileData;
import com.hpe.caf.worker.testing.api.TestDataSource;
import com.hpe.caf.worker.testing.preparation.TestInputFactory;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ploch on 14/04/2017.
 */
public class ReflectingTestInputFactory implements TestInputFactory
{

    private final Class workerTaskClass;

    public ReflectingTestInputFactory(Class workerTaskClass)
    {
        this.workerTaskClass = workerTaskClass;
    }

    @Override
    public Object createTestInput(TestDataSource testDataSource)
    {
        Map<String, Object> testInput = new HashMap<>();

        Field[] fields = workerTaskClass.getDeclaredFields();
        for (Field field : fields) {

        }

        return null;
    }

    private Object getValue(String parentPath, Field field, TestDataSource dataSource)
    {
        if (field.getType() == ReferencedData.class) {

            InputFileData fileData = new InputFileData();
            fileData.setFilePath(dataSource.getData(Path.class).getFileName().toString());
            return fileData;
        }

        throw new UnsupportedOperationException();
    }
}
