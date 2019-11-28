/*
 * Copyright 2015-2020 Micro Focus or one of its affiliates.
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
package com.hpe.caf.worker.testing;

import com.hpe.caf.worker.testing.data.ContentComparisonType;

/**
 * Created by ploch on 25/11/2015.
 */
public class ContentFileTestExpectation
{
    private String expectedContentFile;

    private int expectedSimilarityPercentage;

    private ContentComparisonType comparisonType = ContentComparisonType.TEXT;

    /**
     * Getter for property 'expectedContentFile'.
     *
     * @return Value for property 'expectedContentFile'.
     */
    public String getExpectedContentFile()
    {
        return expectedContentFile;
    }

    /**
     * Setter property 'expectedContentFile'.
     *
     * @param expectedContentFile Value to set for property 'expectedContentFile'.
     */
    public void setExpectedContentFile(String expectedContentFile)
    {
        this.expectedContentFile = expectedContentFile == null ? null : expectedContentFile.replace("\\", "/");
    }

    /**
     * Getter for property 'expectedSimilarityPercentage'.
     *
     * @return Value for property 'expectedSimilarityPercentage'.
     */
    public int getExpectedSimilarityPercentage()
    {
        return expectedSimilarityPercentage;
    }

    /**
     * Setter for property 'expectedSimilarityPercentage'.
     *
     * @param expectedSimilarityPercentage Value to set for property 'expectedSimilarityPercentage'.
     */
    public void setExpectedSimilarityPercentage(int expectedSimilarityPercentage)
    {
        this.expectedSimilarityPercentage = expectedSimilarityPercentage;
    }

    /**
     * Getter for property 'comparisonType'.
     *
     * @return Value for property 'comparisonType'.
     */
    public ContentComparisonType getComparisonType()
    {
        return comparisonType;
    }

    /**
     * Setter for property 'comparisonType'.
     *
     * @param comparisonType Value to set for property 'comparisonType'.
     */
    public void setComparisonType(ContentComparisonType comparisonType)
    {
        this.comparisonType = comparisonType;
    }
}
