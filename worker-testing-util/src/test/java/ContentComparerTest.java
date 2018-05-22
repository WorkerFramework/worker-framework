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
import static com.hpe.caf.worker.testing.Sift4Comparator.sift4Distance_Common;
import static com.hpe.caf.worker.testing.Sift4Comparator.sift4Distance_Simple;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import junit.framework.Assert;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.junit.Test;

/**
 *
 * @author Trevor Getty <trevor.getty@microfocus.com>
 */
public class ContentComparerTest
{
    enum Algorithms
    {
        SIFT_SIMPLE,
        SIFT_COMMON,
        LEVENSTEIN,
        WINKLER
    }

    private static final int MAX_OFFSET_TO_FIND_CHARACTERS = 20;

    // simple seems good enough for most use cases for now, just test its similarity is high enough.
    @Test
    public void runSift4DistanceAlgorithms_largeFile() throws IOException
    {
        System.out.println("runSift4DistanceAlgorithms_largeFile");
        final String source = getStringFromResource("ets.tif.result.content");
        final String source2 = getStringFromResource("ets.tif.result.returned.content");

        final double minPercentage = 99;

        Assert.assertTrue("Sift4 Algorithm: " + Algorithms.SIFT_SIMPLE + " must return > " + minPercentage + "% similarity", calculateSimilarityChoicePercentage(source, source2, MAX_OFFSET_TO_FIND_CHARACTERS, Algorithms.SIFT_SIMPLE) > minPercentage);
        Assert.assertTrue("Sift4 Algorithm: " + Algorithms.SIFT_COMMON + " must return > " + minPercentage + "% similarity", calculateSimilarityChoicePercentage(source, source2, MAX_OFFSET_TO_FIND_CHARACTERS, Algorithms.SIFT_COMMON) > minPercentage);
    }

    @Test
    public void runSift4DistanceAlgorithms_smallFile() throws IOException
    {
        System.out.println("runSift4DistanceAlgorithms_smallFile");

        final String source = getStringFromResource("testSimple.txt");
        final String source2 = getStringFromResource("testSimple_1.txt");

        final double minPercentage = 95;

        Assert.assertTrue("Sift4 Algorithm: " + Algorithms.SIFT_SIMPLE + " must return > " + minPercentage + "% similarity", calculateSimilarityChoicePercentage(source, source2, MAX_OFFSET_TO_FIND_CHARACTERS, Algorithms.SIFT_SIMPLE) > minPercentage);
        Assert.assertTrue("Sift4 Algorithm: " + Algorithms.SIFT_COMMON + " must return > " + minPercentage + "% similarity", calculateSimilarityChoicePercentage(source, source2, MAX_OFFSET_TO_FIND_CHARACTERS, Algorithms.SIFT_COMMON) > minPercentage);
    }

    private String getStringFromResource(final String resourceName, Charset encoding) throws IOException
    {
        InputStream is = this.getClass().getResourceAsStream(resourceName);
        Assert.assertNotNull("source content: " + resourceName, is);
        final String source = IOUtils.toString(is, encoding);
        return source;
    }

    private String getStringFromResource(final String resourceName) throws IOException
    {
        return getStringFromResource(resourceName, StandardCharsets.UTF_8);
    }

    private static double calculateSimilarityChoicePercentage(final String s1, final String s2, final int maxOffset, final Algorithms algorithm)
    {
        System.out.println(DateTime.now().toLocalTime() + " Starting");
        final double percentage = calculateSimilarityChoice(s1, s2, maxOffset, algorithm) * 100;
        System.out.println(DateTime.now().toLocalTime() + " Finished algorithm: " + algorithm + " with similarity percentage: " + percentage);
        return percentage;
    }

    private static double calculateSimilarityChoice(final String s1, final String s2, final int threshold, final Algorithms algorithm)
    {
        String longer = s1, shorter = s2;
        if (s1.length() < s2.length()) { // longer should always have greater length
            longer = s2;
            shorter = s1;
        }
        int longerLength = longer.length();
        if (longerLength == 0) {
            return 1.0;
        }

        switch (algorithm) {
            case WINKLER: {
                return (longerLength - StringUtils.getJaroWinklerDistance(longer, shorter)) / (double) longerLength;
            }
            case LEVENSTEIN: {
                return (longerLength - StringUtils.getLevenshteinDistance(longer, shorter, threshold)) / (double) longerLength;
            }
            case SIFT_SIMPLE: {
                return (longerLength - sift4Distance_Simple(longer, shorter, threshold)) / (double) longerLength;
            }
            case SIFT_COMMON: {
                return (longerLength - sift4Distance_Common(longer, shorter, threshold, 0)) / (double) longerLength;
            }
            default:
                throw new RuntimeException("Unknownh algorith");
        }
    }
}
