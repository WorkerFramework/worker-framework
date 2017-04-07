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
 * Provides information about a file which will be used in a test.
 */
public class InputFileData {

    private String filePath;
    private String storageReference;

    /**
     * Gets file path used by test.
     *
     * @return the file path
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * Sets file path used by test.
     *
     * @param filePath the file path
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    /**
     * Gets storage reference.
     * If storage reference is provided, it will be passed to the worker as it. File Path will not be used.
     *
     * @return the storage reference
     */
    public String getStorageReference() {
        return storageReference;
    }

    /**
     * Sets storage reference.
     * If storage reference is provided, it will be passed to the worker as it. File Path will not be used.
     *
     * @param storageReference the storage reference
     */
    public void setStorageReference(String storageReference) {
        this.storageReference = storageReference;
    }
}
