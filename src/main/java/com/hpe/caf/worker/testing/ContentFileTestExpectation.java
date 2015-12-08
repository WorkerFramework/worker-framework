package com.hpe.caf.worker.testing;

import com.hpe.caf.worker.testing.data.ContentComparisonType;

/**
 * Created by ploch on 25/11/2015.
 */
public class ContentFileTestExpectation {

    private String expectedContentFile;

    private int expectedSimilarityPercentage;

    private ContentComparisonType comparisonType = ContentComparisonType.TEXT;

    /**
     * Getter for property 'expectedContentFile'.
     *
     * @return Value for property 'expectedContentFile'.
     */
    public String getExpectedContentFile() {
        return expectedContentFile;
    }

    /**
     * Setter   property 'expectedContentFile'.
     *
     * @param expectedContentFile Value to set for property 'expectedContentFile'.
     */
    public void setExpectedContentFile(String expectedContentFile) {
        this.expectedContentFile = expectedContentFile;
    }

    /**
     * Getter for property 'expectedSimilarityPercentage'.
     *
     * @return Value for property 'expectedSimilarityPercentage'.
     */
    public int getExpectedSimilarityPercentage() {
        return expectedSimilarityPercentage;
    }

    /**
     * Setter for property 'expectedSimilarityPercentage'.
     *
     * @param expectedSimilarityPercentage Value to set for property 'expectedSimilarityPercentage'.
     */
    public void setExpectedSimilarityPercentage(int expectedSimilarityPercentage) {
        this.expectedSimilarityPercentage = expectedSimilarityPercentage;
    }

    /**
     * Getter for property 'comparisonType'.
     *
     * @return Value for property 'comparisonType'.
     */
    public ContentComparisonType getComparisonType() {
        return comparisonType;
    }

    /**
     * Setter for property 'comparisonType'.
     *
     * @param comparisonType Value to set for property 'comparisonType'.
     */
    public void setComparisonType(ContentComparisonType comparisonType) {
        this.comparisonType = comparisonType;
    }
}
