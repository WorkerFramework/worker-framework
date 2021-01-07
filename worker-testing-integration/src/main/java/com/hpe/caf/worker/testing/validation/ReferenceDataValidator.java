/*
 * Copyright 2015-2021 Micro Focus or one of its affiliates.
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
package com.hpe.caf.worker.testing.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.util.ref.DataSourceException;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.testing.ContentComparer;
import com.hpe.caf.worker.testing.ContentFileTestExpectation;
import com.hpe.caf.worker.testing.TestResultHelper;
import com.hpe.caf.worker.testing.data.ContentComparisonType;
import com.hpe.caf.worker.testing.data.ContentDataHelper;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

/**
 * Created by ploch on 07/12/2015.
 */
public class ReferenceDataValidator extends PropertyValidator
{
    private final boolean throwOnValidationFailure;
    private final DataStore dataStore;
    private final Codec codec;
    private final String testDataFolder;
    private final String testSourcefileBaseFolder;

    public ReferenceDataValidator(DataStore dataStore, Codec codec, String testDataFolder, String testSourcefileBaseFolder)
    {
        this.throwOnValidationFailure = true;
        this.dataStore = dataStore;
        this.codec = codec;
        this.testDataFolder = testDataFolder;
        this.testSourcefileBaseFolder = testSourcefileBaseFolder;
    }

    public ReferenceDataValidator(boolean throwOnValidationFailure, DataStore dataStore, Codec codec, String testDataFolder, String testSourcefileBaseFolder)
    {
        this.throwOnValidationFailure = throwOnValidationFailure;
        this.dataStore = dataStore;
        this.codec = codec;
        this.testDataFolder = testDataFolder;
        this.testSourcefileBaseFolder = testSourcefileBaseFolder;
    }

    @Override
    public boolean isValid(Object testedPropertyValue, Object validatorPropertyValue)
    {
        if (testedPropertyValue == null && validatorPropertyValue == null) {
            return true;
        }

        ObjectMapper mapper = new ObjectMapper();

        ContentFileTestExpectation expectation = mapper.convertValue(validatorPropertyValue, ContentFileTestExpectation.class);

        ReferencedData referencedData = mapper.convertValue(testedPropertyValue, ReferencedData.class);

        InputStream dataStream;

        if (expectation.getExpectedContentFile() == null && expectation.getExpectedSimilarityPercentage() == 0) {
            return true;
        }

        try {
            System.out.println("About to retrieve content for " + referencedData.toString());
            dataStream = ContentDataHelper.retrieveReferencedData(dataStore, codec, referencedData);
            System.out.println("Finished retrieving content for " + referencedData.toString());
        } catch (DataSourceException e) {
            e.printStackTrace();
            System.err.println("Failed to acquire referenced data.");
            e.printStackTrace();
            TestResultHelper.testFailed("Failed to acquire referenced data. Exception message: " + e.getMessage(), e);
            return false;
        }

        try {
            String contentFileName = expectation.getExpectedContentFile();
            Path contentFile = Paths.get(contentFileName);
            if (Files.notExists(contentFile) && !Strings.isNullOrEmpty(testSourcefileBaseFolder)) {
                contentFile = Paths.get(testSourcefileBaseFolder, contentFileName);
            }

            if (Files.notExists(contentFile)) {
                contentFile = Paths.get(testDataFolder, contentFileName);
            }

            byte[] expectedFileBytes = Files.readAllBytes(contentFile);

            if (expectation.getComparisonType() == ContentComparisonType.TEXT) {

                String actualText = IOUtils.toString(dataStream, StandardCharsets.UTF_8);
                String expectedText = new String(expectedFileBytes, StandardCharsets.UTF_8);

                if (expectation.getExpectedSimilarityPercentage() == 100) {
                    boolean equals = actualText.equals(expectedText);
                    if (!equals) {
                        String message = "Expected and actual texts were different.\n\n*** Expected Text ***\n"
                            + expectedText + "\n\n*** Actual Text ***\n" + actualText;
                        System.err.println(message);
                        if (throwOnValidationFailure) {
                            TestResultHelper.testFailed(message);
                        }
                        return false;
                    }
                    return true;
                }

                double similarity = ContentComparer.calculateSimilarityPercentage(expectedText, actualText);

                System.out.println("Compared text similarity:" + similarity + "%");

                if (similarity < expectation.getExpectedSimilarityPercentage()) {
                    String message = "Expected similarity of " + expectation.getExpectedSimilarityPercentage() + "% but actual similarity was " + similarity + "%";
                    System.err.println(message);
                    if (throwOnValidationFailure) {
                        TestResultHelper.testFailed(message);
                    }
                    return false;
                }
            } else {
                byte[] actualDataBytes = IOUtils.toByteArray(dataStream);
                boolean equals = Arrays.equals(actualDataBytes, expectedFileBytes);
                if (!equals) {
                    String actualContentFileName = contentFile.getFileName() + "_actual";
                    Path actualFilePath = Paths.get(contentFile.getParent().toString(), actualContentFileName);
                    Files.deleteIfExists(actualFilePath);
                    Files.write(actualFilePath, actualDataBytes, StandardOpenOption.CREATE);
                    String message
                        = "Data returned was different than expected for file: " + contentFileName
                        + "\nActual content saved in file: " + actualFilePath.toString();
                    System.err.println(message);
                    if (throwOnValidationFailure) {
                        TestResultHelper.testFailed(message);
                    }
                    return false;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            TestResultHelper.testFailed("Error while processing reference data! " + e.getMessage(), e);
            return false;
        }
        return true;
    }
}
