package com.hpe.caf.worker.testing.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.worker.DataStore;
import com.hpe.caf.util.ref.DataSourceException;
import com.hpe.caf.util.ref.ReferencedData;
import com.hpe.caf.worker.testing.ContentComparer;
import com.hpe.caf.worker.testing.ContentFileTestExpectation;
import com.hpe.caf.worker.testing.data.ContentComparisonType;
import com.hpe.caf.worker.testing.data.ContentDataHelper;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Created by ploch on 07/12/2015.
 */
public class ReferenceDataValidator extends PropertyValidator {

    private final DataStore dataStore;
    private final Codec codec;
    private final String testDataFolder;

    public ReferenceDataValidator(DataStore dataStore, Codec codec, String testDataFolder) {

        this.dataStore = dataStore;
        this.codec = codec;
        this.testDataFolder = testDataFolder;
    }

    @Override
    public boolean isValid(Object testedPropertyValue, Object validatorPropertyValue) {

        if (testedPropertyValue == null && validatorPropertyValue == null) return true;

        ObjectMapper mapper = new ObjectMapper();

        ContentFileTestExpectation expectation = mapper.convertValue(validatorPropertyValue, ContentFileTestExpectation.class);

        ReferencedData referencedData = mapper.convertValue(testedPropertyValue, ReferencedData.class);

        InputStream dataStream;

        try {
            System.out.println("About to retrieve content for " + referencedData.toString());
            dataStream = ContentDataHelper.retrieveReferencedData(dataStore, codec, referencedData);
            System.out.println("Finished retrieving content for " + referencedData.toString());
        }
        catch (DataSourceException e) {
            e.printStackTrace();
            System.err.println("Failed to acquire referenced data.");
            return false;
        }
        try {
            String contentFileName = expectation.getExpectedContentFile();
            Path contentFile = Paths.get(contentFileName);
            if (Files.notExists(contentFile)) {
                contentFile = Paths.get(testDataFolder, contentFileName);
            }

            byte[] expectedFileBytes = Files.readAllBytes(contentFile);

            if (expectation.getComparisonType() == ContentComparisonType.TEXT) {

                String actualText = IOUtils.toString(dataStream, StandardCharsets.UTF_8);
                String expectedText = new String(expectedFileBytes);

                if (expectation.getExpectedSimilarityPercentage() == 100) {
                    return actualText.equals(expectedText);
                }

                double similarity = ContentComparer.calculateSimilarityPercentage(expectedText, actualText);

                System.out.println("Compared text similarity:" + similarity + "%");

                if (similarity < expectation.getExpectedSimilarityPercentage()) {
                    System.err.println("Expected similarity of " + expectation.getExpectedSimilarityPercentage() + "% but actual similarity was " + similarity + "%");
                    return false;
                }
            }
            else {
                byte[] actualDataBytes = IOUtils.toByteArray(dataStream);
                boolean equals = Arrays.equals(actualDataBytes, expectedFileBytes);
                if (!equals) {
                    System.err.println("Data returned was different than expected for file: " + contentFileName);
                    return false;
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }
}
