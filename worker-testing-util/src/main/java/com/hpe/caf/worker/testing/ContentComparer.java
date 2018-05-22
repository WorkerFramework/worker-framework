/*
 * Copyright 2018-2017 EntIT Software LLC, a Micro Focus company.
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

/**
 * Used to compare the similarity between 2 strings which may not be identical.
 */
public class ContentComparer
{
    private static final int MAX_CHAR_OFFSET = 20;

    private ContentComparer()
    {
    }

    /**
     * Calculate the similarity between 2 strings.
     *
     * @param s1 First string
     * @param s2 Second string
     * @param thresh Max character offset to search for matches
     * @return similarity distance of the 2 strings.
     */
    public static double calculateSimilarity(final String s1, final String s2, final int thresh)
    {
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) { // longer should always have greater length
            longer = s2;
            shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) {
            return 1.0;
            /* both strings are zero length */ }
        return (longerLength - Sift4Comparator.sift4Distance_Simple(longer, shorter, thresh)) / (double) longerLength;
    }

    /**
     * Calculate the similarity between 2 strings, based on a default character offset ( MAX_CHAR_OFFSET )
     *
     * @param s1 First string
     * @param s2 Second string
     * @return Similarity percentage between the 2 strings.
     */
    public static double calculateSimilarityPercentage(final String s1, final String s2)
    {
        return calculateSimilarityPercentage(s1, s2, MAX_CHAR_OFFSET);
    }

    /**
     * Calculate the similarity between 2 strings.
     *
     * @param s1 First string
     * @param s2 Second string
     * @param thresh Max character offset to search for matches
     * @return Similarity percentage between the 2 strings.
     */
    public static double calculateSimilarityPercentage(final String s1, final String s2, final int thresh)
    {
        return calculateSimilarity(s1, s2, thresh) * 100;
    }

}
