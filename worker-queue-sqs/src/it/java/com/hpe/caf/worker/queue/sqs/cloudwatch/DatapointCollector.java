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

import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatapointCollector
{
    private final CloudWatchClient cloudWatch;

    public DatapointCollector(final CloudWatchClient cloudWatch)
    {
        this.cloudWatch = cloudWatch;
    }

    public Map<String, List<MetricDataPoints>> sqsDatapoints(
            final String queue,
            final Statistic statistic,
            final String... metrics)
    {
        final var datapoints = new HashMap<String, List<MetricDataPoints>>();
        Calendar current = Calendar.getInstance();
        current.set(current.get(Calendar.YEAR), current.get(Calendar.MONTH), current.get(Calendar.DATE), 0, 0, 0);
        datapoints.put(queue, new ArrayList<>());
        for(final var metric : metrics) {
            final var statsRequest = GetMetricStatisticsRequest.builder()
                    .namespace("AWS/SQS")
                    .dimensions(Dimension.builder().name("QueueName").value(queue).build())
                    .metricName(metric)
                    .startTime(current.toInstant())
                    .endTime(Instant.now())
                    .statistics(statistic)
                    .period(600)
                    .build();
            final var statsResponse = cloudWatch.getMetricStatistics(statsRequest);
            datapoints.get(queue).add(new MetricDataPoints(metric, statsResponse.datapoints()));
        }
        return datapoints;
    }
}
