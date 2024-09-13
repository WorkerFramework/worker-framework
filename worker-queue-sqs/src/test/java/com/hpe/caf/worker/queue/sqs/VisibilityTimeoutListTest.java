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
import org.testng.annotations.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.testng.AssertJUnit.assertTrue;

public class VisibilityTimeoutListTest
{
    @Test
    public void testSortedSetOfVisibilityTimeouts()
    {
        var startTime = Instant.now().getEpochSecond();
        final List<VisibilityTimeout> visibilityTimeouts = Collections.synchronizedList(new ArrayList<>());
        for(int i = 1; i < 100; i++) {
            var ti = getVisibilityTimeout(startTime, i);
            visibilityTimeouts.add(ti);
        }
        VisibilityTimeout prev = null;
        Collections.sort(visibilityTimeouts);

        for(VisibilityTimeout next : visibilityTimeouts) {
            if (prev != null) {
                assertTrue(prev.getBecomesVisibleEpochSecond() < next.getBecomesVisibleEpochSecond());
            }
            prev = next;
        }
    }

    private VisibilityTimeout getVisibilityTimeout(final Long start, final int offset)
    {
        return new VisibilityTimeout(
                new QueueInfo("a", "b", "c"),
                (offset % 2 == 0) ? start + offset: start - offset,
                UUID.randomUUID().toString()
        );
    }
}
