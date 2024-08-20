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

import com.hpe.caf.worker.queue.sqs.util.DatapointCollector;
import com.hpe.caf.worker.queue.sqs.util.MetricDataPoints;
import com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper.getWorkerWrapper;
import static com.hpe.caf.worker.queue.sqs.util.WorkerQueueWrapper.sendMessages;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MetricsIT
{
    @Test
    public void testMetricsShowReceivedAndDeleted() throws Exception
    {
        final var inputQueue = "test-deleted-metrics";
        final var workerWrapper = getWorkerWrapper(inputQueue);

        var messagesToSend = 10;
        for (int i = 1; i <= messagesToSend; i++) {
            sendMessages(workerWrapper.sqsClient, workerWrapper.inputQueueUrl, new HashMap<>(), "msg_" + i);
        }

        var datapoints = getStatistics(workerWrapper);

        // Receive all messages
        var msg = workerWrapper.callbackQueue.poll(30, TimeUnit.SECONDS);
        while (msg != null) {
            workerWrapper.sqsWorkerQueue.acknowledgeTask(msg.taskInformation());
            msg = workerWrapper.callbackQueue.poll(30, TimeUnit.SECONDS);
        }

        assertTrue(datapoints.containsKey(inputQueue));
        var metricsList = datapoints.get(inputQueue);

        assertTrue(containsMetric(metricsList, "NumberOfMessagesSent"));
        assertTrue(containsMetric(metricsList, "NumberOfMessagesReceived"));
        assertTrue(containsMetric(metricsList, "NumberOfMessagesDeleted"));
        assertTrue(containsMetric(metricsList, "ApproximateNumberOfMessagesNotVisible"));
        assertTrue(containsMetric(metricsList, "ApproximateNumberOfMessagesVisible"));
        assertTrue(containsMetric(metricsList, "ApproximateAgeOfOldestMessage"));
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
