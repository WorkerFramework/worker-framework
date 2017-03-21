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
package com.hpe.caf.worker.testing;

import com.google.common.base.Strings;
import java.util.ArrayList;
import org.apache.commons.lang3.StringUtils;

/**
 * Used to compare the similarity between 2 strings which may not be identical.
 */
public class ContentComparer {

    private ContentComparer(){}

    private static final int MAX_CHAR_OFFSET = 20;
    
    public static double calculateSimilarity(final String s1, final String s2, final int thresh) {
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) { // longer should always have greater length
            longer = s2; shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) { return 1.0; /* both strings are zero length */ }
        return (longerLength - Sift4Comparator.sift4Distance_Simple(longer, shorter, thresh)) / (double) longerLength;
    }

    public static double calculateSimilarityPercentage(final String s1, final String s2){
        return calculateSimilarityPercentage(s1, s2, MAX_CHAR_OFFSET);
    }
    
    public static double calculateSimilarityPercentage(final String s1, final String s2, final int thresh){
        return calculateSimilarity(s1, s2, thresh) * 100;
    }
    
   
}
