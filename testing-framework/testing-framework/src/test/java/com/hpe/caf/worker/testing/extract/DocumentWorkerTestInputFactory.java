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
package com.hpe.caf.worker.testing.extract;

import com.hpe.caf.worker.testing.api.InputFileData;
import com.hpe.caf.worker.testing.api.TestDataSource;
import com.hpe.caf.worker.testing.preparation.TestDataSourceIds;
import com.hpe.caf.worker.testing.preparation.TestInputFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ploch on 20/04/2017.
 */
public class DocumentWorkerTestInputFactory implements TestInputFactory
{
    private final String contentFileField;

    public DocumentWorkerTestInputFactory(String contentFileField)
    {
        this.contentFileField = contentFileField;
    }

    @Override
    public Object createTestInput(TestDataSource testDataSource)
    {
      //  DocumentWorkerTask task = new DocumentWorkerTask();
        HashMap<String, List<Object>> fieldsMap = new HashMap<>();
        HashMap<String, String> customDataMap = new HashMap<>();

        Path file = testDataSource.getData(Path.class, TestDataSourceIds.CONTENT_FILE);
        if (file != null) {
            List<Object> fieldValues = new ArrayList<>();
            InputFileData fileData = new InputFileData();
            fileData.setFilePath(file.getFileName().toString());

            fieldValues.add(fileData);
            fieldsMap.put(contentFileField, fieldValues);
        }
        customDataMap.put("outputPartialReference", "TST");

        Map<String, Object> result = new HashMap<>();

        result.put("fields", fieldsMap);
        result.put("customData", customDataMap);

        return result;

    }
}
