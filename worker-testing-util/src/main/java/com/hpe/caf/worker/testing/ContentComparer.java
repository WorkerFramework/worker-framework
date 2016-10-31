package com.hpe.caf.worker.testing;

import org.apache.commons.lang3.StringUtils;

/**
 * Created by ploch on 25/11/2015.
 */
public class ContentComparer {

    private ContentComparer(){}

    public static double calculateSimilarity(String s1, String s2) {
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) { // longer should always have greater length
            longer = s2; shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) { return 1.0; /* both strings are zero length */ }
        return (longerLength - StringUtils.getLevenshteinDistance(longer, shorter)) / (double) longerLength;
    }

    public static double calculateSimilarityPercentage(String s1, String s2){
        return calculateSimilarity(s1, s2) * 100;
    }
}
