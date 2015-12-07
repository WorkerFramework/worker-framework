package com.hpe.caf.worker.testing;

/**
 * Created by ploch on 25/11/2015.
 */
public class ContentFileTestExpectation {

    private String expectedContentFile;

    private int expectedSimilarityPercentage;

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
}
