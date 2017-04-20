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
package com.hpe.caf.worker.testing.experiment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.api.worker.DataStoreException;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.binaryhash.BinaryHashWorkerResult;
import com.hpe.caf.worker.binaryhash.BinaryHashWorkerTask;
import com.hpe.caf.worker.testing.api.InputFileData;
import com.hpe.caf.worker.testing.api.TestItem;
import com.hpe.caf.worker.testing.api.WorkerTaskFactory;

import java.nio.file.Paths;

/**
 * Created by ploch on 14/04/2017.
 */
public class BinaryHashWorkerTaskFactory implements WorkerTaskFactory<BinaryHashWorkerTestInput, BinaryHashWorkerResult>
{

    private final DataStore dataStore;

    public BinaryHashWorkerTaskFactory(DataStore dataStore)
    {
        this.dataStore = dataStore;
    }

    @Override
    public Object createTask(TestItem<BinaryHashWorkerTestInput, BinaryHashWorkerResult> testItem) throws DataStoreException
    {
        BinaryHashWorkerTask workerTask = new BinaryHashWorkerTask();
        ObjectMapper mapper = new ObjectMapper();
        BinaryHashWorkerTestInput binaryHashWorkerTestInput = mapper.convertValue(testItem.getInputData(), BinaryHashWorkerTestInput.class);
        InputFileData fileData = binaryHashWorkerTestInput.getInputFileData();

        String path = Paths.get(testItem.getLocation()).getParent().toAbsolutePath().toString();
        if (!Strings.isNullOrEmpty(fileData.getStorageReference())) {
            workerTask.sourceData = ReferencedData.getReferencedData(fileData.getStorageReference());
        }
        else {
            String reference = dataStore.store(Paths.get(path, fileData.getFilePath()), null);
            workerTask.sourceData = ReferencedData.getReferencedData(reference);
        }
        return workerTask;
    }
}
