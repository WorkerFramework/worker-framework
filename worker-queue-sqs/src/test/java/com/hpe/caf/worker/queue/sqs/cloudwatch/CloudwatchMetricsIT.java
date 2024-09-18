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
package com.hpe.caf.worker.queue.sqs.cloudwatch;

import com.hpe.caf.worker.queue.sqs.TestContainer;
import com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper;
import org.testng.annotations.Test;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;

import java.util.List;
import java.util.Map;

import static org.testng.AssertJUnit.assertTrue;

/**
 * Testing AWS cloudwatch metrics availability, not to be confused with MetricsReporter functionality.
 */
public class CloudwatchMetricsIT extends TestContainer
{
    @Test
    public void testMetricsShowReceivedAndDeleted() throws Exception
    {
        final var inputQueue = "test-deleted-metrics";
        final var workerWrapper = getWorkerWrapper(inputQueue);

        try {
            var datapoints = getStatistics(workerWrapper);

            assertTrue(datapoints.containsKey(inputQueue));
            var metricsList = datapoints.get(inputQueue);

            assertTrue(containsMetric(metricsList, "NumberOfMessagesSent"));
            assertTrue(containsMetric(metricsList, "NumberOfMessagesReceived"));
            assertTrue(containsMetric(metricsList, "NumberOfMessagesDeleted"));
            assertTrue(containsMetric(metricsList, "ApproximateNumberOfMessagesNotVisible"));
            assertTrue(containsMetric(metricsList, "ApproximateNumberOfMessagesVisible"));
            assertTrue(containsMetric(metricsList, "ApproximateAgeOfOldestMessage"));
        } finally {
            workerWrapper.sqsWorkerQueue.shutdown();
        }
    }

    private Map<String, List<MetricDataPoints>> getStatistics(
            final WorkerQueueWrapper workerWrapper
    )
    {
        final CloudWatchClient cloudWatch = workerWrapper.getCloudwatchClient();
        final var datapointCollector = new DatapointCollector(cloudWatch);
        return datapointCollector.sqsDatapoints(
                workerWrapper.sqsWorkerQueue.getInputQueue(),
                software.amazon.awssdk.services.cloudwatch.model.Statistic.SUM,
                "NumberOfMessagesSent",
                "NumberOfMessagesReceived",
                "NumberOfMessagesDeleted",
                "ApproximateNumberOfMessagesVisible",
                "ApproximateNumberOfMessagesNotVisible",
                "ApproximateAgeOfOldestMessage"
        );
    }

    private boolean containsMetric(final List<MetricDataPoints> datapoints, final String metricName)
    {
        var metrics = datapoints
                .stream()
                .filter(dp -> dp.metric().equals(metricName))
                .findFirst();
        return metrics.isPresent();
    }
}
