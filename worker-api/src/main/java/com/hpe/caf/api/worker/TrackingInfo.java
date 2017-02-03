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
package com.hpe.caf.api.worker;

import java.text.MessageFormat;
import java.util.Date;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Holds fields used in tracking task messages, for Progress Reporting and Job Control.
 */
public class TrackingInfo {
    /**
     * Values of jobTaskId should be the job id followed by period-separated subtask elements.
     * For example J5.1.2 where the job id is "J5".
     */
    private static final String jobTaskIdPattern = "^([^\\.]*)\\.?.*$";

    /**
     * An identifier assigned for tracking the task - not the same as the taskId on TaskMessage.
     * This identifier should match the format specified by TrackingInfo.getJobTaskIdPattern()
     */
    private String jobTaskId;

    /**
     * The time after which it is appropriate to try to confirm that the task has not been cancelled or aborted.
     */
    private Date statusCheckTime;

    /**
     * The url to use to check whether the job has been cancelled or aborted.
     */
    private String statusCheckUrl;

    /**
     * The pipe to which output messages relating to this task should be sent, regardless of their nature
     * (i.e. whether they are Reject messages, Retry messages, Response messages, or some other type of message).
     * It is the responsibility of the Job Tracking Worker, which will be consuming messages sent to this pipe, to
     * forward the message to the intended recipient, which is indicated by the TaskMessage.to field.
     * NOTE: One exception to this is where the tracking pipe specified is the same pipe that the worker itself is
     *       consuming messages from. If this is the case then the tracking pipe should be ignored. It likely means
     *       that this is the Job Tracking Worker. Not making an exception for this case would cause to an
     *       infinite loop.
     */
    private String trackingPipe;

    /**
     * The pipe where tracking is to stop.
     * If the Worker Framework is publishing a message to this pipe then it should remove the 'tracking' fields,
     * as we are not interested in tracking from this point.
     */
    private String trackTo;


    public TrackingInfo() {
    }


    public TrackingInfo(String jobTaskId, Date statusCheckTime, String statusCheckUrl, String trackingPipe, String trackTo) {
        this.jobTaskId = jobTaskId;
        this.statusCheckTime = statusCheckTime;
        this.statusCheckUrl = statusCheckUrl;
        this.trackingPipe = trackingPipe;
        this.trackTo = trackTo;
    }


    public String getJobTaskId() {
        return jobTaskId;
    }


    public void setJobTaskId(String jobTaskId) {
        this.jobTaskId = jobTaskId;
    }


    public Date getStatusCheckTime() {
        return statusCheckTime;
    }


    public void setStatusCheckTime(Date statusCheckTime) {
        this.statusCheckTime = statusCheckTime;
    }


    public String getStatusCheckUrl() {
        return statusCheckUrl;
    }


    public void setStatusCheckUrl(String statusCheckUrl) {
        this.statusCheckUrl = statusCheckUrl;
    }


    public String getTrackingPipe() {
        return trackingPipe;
    }


    public void setTrackingPipe(String trackingPipe) {
        this.trackingPipe = trackingPipe;
    }


    public String getTrackTo() {
        return trackTo;
    }


    public void setTrackTo(String trackTo) {
        this.trackTo = trackTo;
    }


    /**
     * Extracts the job identifier from the tracking info's job task identifier.
     * @return the extracted job identifier
     */
    public String getJobId() throws InvalidJobTaskIdException {
        Objects.requireNonNull(getJobTaskId());

        Pattern pattern = Pattern.compile(jobTaskIdPattern);
        Matcher matcher = pattern.matcher(getJobTaskId());
        if (matcher.find()) {
            return matcher.group(1);
        }

        throw new InvalidJobTaskIdException(MessageFormat.format("Failed to extract the job identifier from the job task ID {0}", getJobTaskId()));
    }
}
