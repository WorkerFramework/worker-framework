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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.worker.document.DocumentWorkerFieldEncoding;
import com.hpe.caf.worker.testing.api.InputFileData;
import com.hpe.caf.worker.testing.api.TestItem;
import com.hpe.caf.worker.testing.api.WorkerTaskFactory;
import com.hpe.caf.worker.testing.storage.TestCaseSerializer;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ploch on 20/04/2017.
 */
public class DocumentWorkerTaskFactory implements WorkerTaskFactory
{
    private final TestCaseSerializer serializer;
    private final DataStore dataStore;

    public DocumentWorkerTaskFactory(DataStore dataStore)
    {
        this(TestCaseSerializer.defaultSerializer, dataStore);
    }

    public DocumentWorkerTaskFactory(TestCaseSerializer serializer, DataStore dataStore)
    {

        this.serializer = serializer;
        this.dataStore = dataStore;
    }
    @Override
    public Object createTask(TestItem testItem) throws DataStoreException
    {
        Map<String, Object> inputData = (Map<String, Object>) testItem.getInputData();

        HashMap<String, Object> task = new HashMap<>();
        processLevel(inputData, task);

        for (Map.Entry<String, Object> entry : inputData.entrySet()) {
            Object value = entry.getValue();
            String key = entry.getKey();
            if (value != null) {
                Class<?> aClass = value.getClass();

            }
        }



        //
      //  DocumentWorkerTask task = new DocumentWorkerTask();

        return null;
    }

    private void processLevel(Map<String, Object> sourcemap, Map<String, Object> target)
    {

        for (Map.Entry<String, Object> entry : sourcemap.entrySet()) {
            Object value = entry.getValue();
            String key = entry.getKey();

            Object targetValue;


            //DocumentWorkerFieldValue targetValue = new DocumentWorkerFieldValue();

            ObjectMapper mapper = new ObjectMapper();

            if (value != null) {
                Class<?> aClass = value.getClass();
                try {
                    InputFileData fileData = mapper.convertValue(value, InputFileData.class);
                 //   InputFileData convert = ReflectUtils.getInstance().convert(value, InputFileData.class);
                    String reference = dataStore.store(Paths.get(fileData.getFilePath()), "TST");
                    targetValue.encoding = DocumentWorkerFieldEncoding.storage_ref;
                    targetValue.data = reference;
                    target.put(key, fileData);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                /*if (value instanceof InputFileData) {
                    InputFileData fileData = (InputFileData) value;
                    value = ReferencedData.getReferencedData("");
                }*/
                if (value instanceof Iterable) {
                    Iterable sourceIterableValue = (Iterable) value;
                    targetValue = new ArrayList<>();
                    for (Object sourceeCollectionValue : sourceIterableValue) {

                    }


                }
                if (value instanceof Map) {
                    Map valueMap = (Map) value;
                    HashMap<String, Object> targetMap = new HashMap<>();
                    processLevel(valueMap, targetMap);
                    value = targetMap;
                }
                target.put(key, value);
            }
        }
    }

    private Object getTargetValue(Object sourceValue)
    {

    }


}
