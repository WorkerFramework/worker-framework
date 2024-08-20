/*
 * Copyright 2015-2024 Open Text.
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
package com.hpe.caf.worker.queue.sqs;

import com.hpe.caf.worker.queue.sqs.visibility.VisibilityTimeout;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class VisibilityTimeoutTest
{
    @Test
    public void testSortedSetOfVisibilityTimeouts()
    {
        var startTime = Instant.now();
        final SortedSet<VisibilityTimeout> set = Collections.synchronizedSortedSet(new TreeSet<>());
        for(int i = 1; i < 100; i++) {
            var ti = getVisibilityTimeout(startTime, i);
            set.add(ti);
        }
        VisibilityTimeout prev = null;

        for(VisibilityTimeout next : set) {
            if (prev != null) {
                assertTrue(prev.getBecomesVisible().isBefore(next.getBecomesVisible()));
            }
            prev = next;
        }
    }

    private VisibilityTimeout getVisibilityTimeout(final Instant start, final int offset)
    {
        return new VisibilityTimeout(
                (offset % 2 == 0) ? start.plusSeconds(offset): start.minusSeconds(offset),
                UUID.randomUUID().toString()
        );
    }
}
