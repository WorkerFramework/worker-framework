/*
 * Copyright 2015-2017 EntIT Software LLC, a Micro Focus company.
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

/**
 * Sift4Comparator which uses the sift4 algorithms to allow string distance comparisons to take place efficiently.
 *
 *
 */
public class Sift4Comparator
{
    /**
     * sift4 simple algorithm Based on JS algorithm @ https://siderite.blogspot.com/2014/11/super-fast-and-accurate-string-distance.html
     *
     * @param s1 First String to compare
     * @param s2 Second String to compare
     * @param maxOffsetValue is the number of characters to search for matching letters
     * @return Distance between the 2 strings.
     */
    public static int sift4Distance_Simple(final String s1, final String s2, final Integer maxOffsetValue)
    {
        final int maxOffset = maxOffsetValue == null ? 5 : maxOffsetValue; //default

        if (Strings.isNullOrEmpty(s1)) {
            if (s2 == null) {
                return 0;
            }
            return s2.length();
        }

        if (Strings.isNullOrEmpty(s2)) {
            return s1.length();
        }

        final int l1 = s1.length();
        final int l2 = s2.length();

        int c1 = 0;  //cursor for string 1
        int c2 = 0;  //cursor for string 2
        int lcss = 0;  //largest common subsequence
        int local_cs = 0; //local common substring
        int trans = 0;  //number of transpositions ('ab' vs 'ba')
        ArrayList<OffsetObject> offset_arr = new ArrayList<>();

        while ((c1 < l1) && (c2 < l2)) {
            if (s1.charAt(c1) == s2.charAt(c2)) {
                local_cs++;
                boolean isTrans = false;
                //see if current match is a transposition
                int i = 0;
                while (i < offset_arr.size()) {
                    OffsetObject ofs = offset_arr.get(i);
                    if (c1 <= ofs.c1 || c2 <= ofs.c2) {
                        // when two matches cross, the one considered a transposition is the one with the largest difference in offsets
                        isTrans = Math.abs(c2 - c1) >= Math.abs(ofs.c2 - ofs.c1);
                        if (isTrans) {
                            trans++;
                        } else if (!ofs.trans) {
                            ofs.trans = true;
                            trans++;
                        }
                        break;
                    } else if (c1 > ofs.c2 && c2 > ofs.c1) {
                        offset_arr.remove(i);
                    } else {
                        i++;
                    }
                }
                offset_arr.add(
                    new OffsetObject(c1, c2, trans));
            } else {
                lcss += local_cs;
                local_cs = 0;
                if (c1 != c2) {
                    c1 = c2 = Math.min(c1, c2);  //using min allows the computation of transpositions
                }
                //if matching characters are found, remove 1 from both cursors (they get incremented at the end of the loop)
                //so that we can have only one code block handling matches 
                for (int j = 0; j < maxOffset && (c1 + j < l1 || c2 + j < l2); j++) {
                    if ((c1 + j < l1) && (s1.charAt(c1 + j) == s2.charAt(c2))) {
                        c1 += j - 1;
                        c2--;
                        break;
                    }
                    if ((c2 + j < l2) && (s1.charAt(c1) == s2.charAt(c2 + j))) {
                        c1--;
                        c2 += j - 1;
                        break;
                    }
                }
            }
            c1++;
            c2++;
            // this covers the case where the last match is on the last token in list, so that it can compute transpositions correctly
            if ((c1 >= l1) || (c2 >= l2)) {
                lcss += local_cs;
                local_cs = 0;
                c1 = c2 = Math.min(c1, c2);
            }
        }
        lcss += local_cs;
        return Math.round(Math.max(l1, l2) - lcss + trans); //add the cost of transpositions to the final result
    }

    /**
     * Sift4 - common version Based on JS algorithm @ https://siderite.blogspot.com/2014/11/super-fast-and-accurate-string-distance.html
     *
     * @param s1 First String to compare
     * @param s2 Second String to compare
     * @param maxOffset is the number of characters to search for matching letters
     * @param maxDistance is the distance at which the algorithm should stop computing the value and just exit (the strings are too
     * different anyway)
     * @return Distance between the 2 strings.
     */
    public static int sift4Distance_Common(final String s1, final String s2, final int maxOffset, final int maxDistance)
    {
        if (Strings.isNullOrEmpty(s1)) {
            if (s2 == null) {
                return 0;
            }
            return s2.length();
        }

        if (Strings.isNullOrEmpty(s2)) {
            return s1.length();
        }

        final int l1 = s1.length();
        final int l2 = s2.length();

        int c1 = 0;  //cursor for string 1
        int c2 = 0;  //cursor for string 2
        int lcss = 0;  //largest common subsequence
        int local_cs = 0; //local common substring
        int trans = 0;  //number of transpositions ('ab' vs 'ba')
        ArrayList<OffsetObject> offset_arr = new ArrayList<>(); //offset pair array, for computing the transpositions

        while ((c1 < l1) && (c2 < l2)) {
            if (s1.charAt(c1) == s2.charAt(c2)) {
                local_cs++;
                boolean isTrans = false;
                //see if current match is a transposition
                int i = 0;
                while (i < offset_arr.size()) {
                    OffsetObject ofs = offset_arr.get(i);
                    if (c1 <= ofs.c1 || c2 <= ofs.c2) {
                        // when two matches cross, the one considered a transposition is the one with the largest difference in offsets
                        isTrans = Math.abs(c2 - c1) >= Math.abs(ofs.c2 - ofs.c1);
                        if (isTrans) {
                            trans++;
                        } else if (!ofs.trans) {
                            ofs.trans = true;
                            trans++;
                        }
                        break;
                    } else if (c1 > ofs.c2 && c2 > ofs.c1) {
                        offset_arr.remove(i);
                    } else {
                        i++;
                    }
                }
                offset_arr.add(new OffsetObject(c1, c2, isTrans));
            } else {
                lcss += local_cs;
                local_cs = 0;
                if (c1 != c2) {
                    c1 = c2 = Math.min(c1, c2);  //using min allows the computation of transpositions
                }
                //if matching characters are found, remove 1 from both cursors (they get incremented at the end of the loop)
                //so that we can have only one code block handling matches 
                for (int i = 0; i < maxOffset && (c1 + i < l1 || c2 + i < l2); i++) {
                    if ((c1 + i < l1) && (s1.charAt(c1 + i) == s2.charAt(c2))) {
                        c1 += i - 1;
                        c2--;
                        break;
                    }
                    if ((c2 + i < l2) && (s1.charAt(c1) == s2.charAt(c2 + i))) {
                        c1--;
                        c2 += i - 1;
                        break;
                    }
                }
            }
            c1++;
            c2++;
            if (maxDistance != 0) {
                int temporaryDistance = Math.max(c1, c2) - lcss + trans;
                if (temporaryDistance >= maxDistance) {
                    return Math.round(temporaryDistance);
                }
            }
            // this covers the case where the last match is on the last token in list, so that it can compute transpositions correctly
            if ((c1 >= l1) || (c2 >= l2)) {
                lcss += local_cs;
                local_cs = 0;
                c1 = c2 = Math.min(c1, c2);
            }
        }
        lcss += local_cs;
        return Math.round(Math.max(l1, l2) - lcss + trans); //add the cost of transpositions to the final result
    }

    /**
     * Internal class to hold the information about the distance or offset between 2 characters.
     */
    private static class OffsetObject
    {
        int c1;
        int c2;
        boolean trans;

        public OffsetObject(final int c1, final int c2, final boolean isTrans)
        {
            this.c1 = c1;
            this.c2 = c2;
            this.trans = isTrans;
        }

        public OffsetObject(final int c1, final int c2, final int trans)
        {
            this.c1 = c1;
            this.c2 = c2;
            this.trans = trans != 0;
        }
    }
}
