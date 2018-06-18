/*
 * Copyright 2015-2018 Micro Focus or one of its affiliates.
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
package com.hpe.caf.worker.core;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.DecodeMethod;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.naming.ServicePath;
import com.hpe.caf.util.rabbitmq.RabbitHeaders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;

/**
 * WorkerCore represents the main logic of the microservice worker. It is responsible for accepting new tasks from a WorkerQueue, handing
 * them off to a backend Worker and executing them upon a thread pool. It will then accept a result from the Worker it executed and hand
 * the TaskResult back to the WorkerQueue for publishing.
 */
final class WorkerCore
{
    private final WorkerThreadPool threadPool;
    private final ManagedWorkerQueue workerQueue;
    private final WorkerStats stats = new WorkerStats();
    private final TaskCallback callback;
    private static final Logger LOG = LoggerFactory.getLogger(WorkerCore.class);

    public WorkerCore(final Codec codec, final WorkerThreadPool pool, final ManagedWorkerQueue queue, final MessagePriorityManager priorityManager, final WorkerFactory factory, final ServicePath path, final HealthCheckRegistry healthCheckRegistry, final TransientHealthCheck transientHealthCheck)
    {
        WorkerCallback taskCallback = new CoreWorkerCallback(codec, queue, stats, healthCheckRegistry, transientHealthCheck);
        this.threadPool = Objects.requireNonNull(pool);
        this.callback = new CoreTaskCallback(codec, stats, new WorkerExecutor(path, taskCallback, factory, pool, priorityManager), pool, queue);
        this.workerQueue = Objects.requireNonNull(queue);
    }

    /**
     * Begin accepting tasks to process.
     *
     * @throws QueueException if the queues cannot be started
     */
    public void start()
        throws QueueException
    {
        workerQueue.start(callback);
    }

    /**
     * The current idle time for the worker. If there are any active threads, this is 0. Otherwise it is the difference between the
     * current time and completion time of the last completed task.
     *
     * @return the current idle time in milliseconds
     */
    public long getCurrentIdleTime()
    {
        if (threadPool.isIdle()) {
            return System.currentTimeMillis() - stats.getLastTaskFinishedTime();
        } else {
            return 0;   // if we are working, then we are not idle
        }
    }

    public ManagedWorkerQueue getWorkerQueue()
    {
        return this.workerQueue;
    }

    /**
     * @return the current number of tasks accepted by the worker but are not in progress
     */
    public int getBacklogSize()
    {
        return threadPool.getBacklogSize();
    }

    public WorkerStats getStats()
    {
        return this.stats;
    }

    /**
     * Called by the queue component to register a new task incoming.
     */
    private static class CoreTaskCallback implements TaskCallback
    {
        private final Codec codec;
        private final WorkerStats stats;
        private final WorkerExecutor executor;
        private final WorkerThreadPool threadPool;
        private final ManagedWorkerQueue workerQueue;

        public CoreTaskCallback(final Codec codec, final WorkerStats stats, final WorkerExecutor executor, final WorkerThreadPool pool, final ManagedWorkerQueue workerQueue)
        {
            this.codec = Objects.requireNonNull(codec);
            this.stats = Objects.requireNonNull(stats);
            this.executor = Objects.requireNonNull(executor);
            this.threadPool = Objects.requireNonNull(pool);
            this.workerQueue = Objects.requireNonNull(workerQueue);
        }

        /**
         * {@inheritDoc}
         *
         * Use the factory to get a new worker to handle the task, wrap this in a handler and hand it off to the thread pool.
         */
        @Override
        public void registerNewTask(final String queueMsgId, final byte[] taskMessage, Map<String, Object> headers)
            throws InvalidTaskException, TaskRejectedException
        {
            Objects.requireNonNull(queueMsgId);
            stats.incrementTasksReceived();
            stats.getInputSizes().update(taskMessage.length);

            try {
                registerNewTaskImpl(queueMsgId, taskMessage, headers);
            } catch (InvalidTaskException e) {
                stats.incrementTasksRejected();
                throw e;
            }
        }

        private void registerNewTaskImpl(final String queueMsgId, final byte[] taskMessage, Map<String, Object> headers)
            throws InvalidTaskException, TaskRejectedException
        {
            try {
                TaskMessage tm = codec.deserialise(taskMessage, TaskMessage.class, DecodeMethod.LENIENT);

                LOG.debug("Received task {} (message id: {})", tm.getTaskId(), queueMsgId);

                boolean poison = isTaskPoisoned(headers);
                validateTaskMessage(tm);
                boolean taskIsActive = checkStatus(tm);
                if (taskIsActive) {
                    if (tm.getTo() != null && tm.getTo().equalsIgnoreCase(workerQueue.getInputQueue())) {
                        LOG.debug("Task {} (message id: {}) on input queue {} {}", tm.getTaskId(), queueMsgId, workerQueue.getInputQueue(), (tm.getTo() != null) ? "is intended for this worker" : "has no explicit destination, therefore assuming it is intended for this worker");
                        executor.executeTask(tm, queueMsgId, poison, headers, codec);
                    } else {
                        LOG.debug("Task {} (message id: {}) is not intended for this worker: input queue {} does not match message destination queue {}", tm.getTaskId(), queueMsgId, workerQueue.getInputQueue(), tm.getTo());
                        executor.forwardTask(tm, queueMsgId, headers);
                    }
                } else {
                    LOG.debug("Task {} is no longer active. The task message (message id: {}) will not be executed", tm.getTaskId(), queueMsgId);
                    executor.discardTask(tm, queueMsgId);
                }
            } catch (CodecException e) {
                throw new InvalidTaskException("Queue data did not deserialise to a TaskMessage", e);
            } catch (InvalidJobTaskIdException ijte) {
                throw new InvalidTaskException("TaskMessage contains an invalid job task identifier", ijte);
            }
        }

        /**
         * Check the headers for retry limit and retry count. If retry count is greater than or equal to retry limit, mark the message as
         * poisoned.
         *
         * @param headers Map&lt;String, Object&gt; of headers associated with the current message
         * @return boolean true if message is determined to be poisoned
         */
        private boolean isTaskPoisoned(Map<String, Object> headers)
        {
            int retryLimit = 0;
            if (null != headers.get(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY_LIMIT)) {
                retryLimit = Integer.parseInt(headers.get(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY_LIMIT).toString());
            }
            int retries = 0;
            if (null != headers.get(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY)) {
                retries = Integer.parseInt(headers.get(RabbitHeaders.RABBIT_HEADER_CAF_WORKER_RETRY).toString());
            }
            boolean poison = false;
            if (retryLimit > 0 && retries > 0 && retries >= retryLimit) {
                poison = true;
            }
            return poison;
        }

        private void validateTaskMessage(TaskMessage tm) throws InvalidTaskException
        {
            // The task message must be present so that the framework can
            // callback with a valid message
            final String taskId = tm.getTaskId();
            if (taskId == null) {
                throw new InvalidTaskException("Task identifier not specified");
            }
        }

        /**
         * Cancel all the Future objects in our Map of running tasks. If the task is not yet running it will just be thrown out of the
         * queue. If it has completed this has no effect. If it is running the Thread will be interrupted.
         */
        @Override
        public void abortTasks()
        {
            LOG.warn("Aborting all current queued and in-progress tasks");
            final int numberOfTasksAborted = threadPool.abortTasks();
            stats.incrementTasksAborted(numberOfTasksAborted);
        }

        /**
         * Checks whether a task is still active. If a status check cannot be performed then the task is assumed to be active. Checking
         * status may result in a change to the tracking info on the supplied task message.
         *
         * @param tm task message to be checked to verify whether the task is still active
         * @return true if the task is still active, false otherwise
         */
        private boolean checkStatus(TaskMessage tm) throws InvalidJobTaskIdException
        {
            Objects.requireNonNull(tm);

            TrackingInfo tracking = tm.getTracking();
            if (tracking != null) {
                Date statusCheckTime = tracking.getStatusCheckTime();
                if (statusCheckTime == null || statusCheckTime.getTime() <= System.currentTimeMillis()) {
                    return performJobStatusCheck(tm);
                }
                LOG.debug("Task {} active status is not being checked - it is not yet time for the status check to be performed: status check due at {}", tm.getTaskId(), statusCheckTime);
            } else {
                LOG.debug("Task {} active status is not being checked - the task message does not have tracking info", tm.getTaskId());
            }

            //By default a task is considered to be active.
            return true;
        }

        /**
         * Checks the current active status of the job to which the task belongs. If this check can be made successfully then the status
         * check time of the supplied task message is updated.
         *
         * @param tm the task message whose job status will be verified
         * @return true if the task's job is active or the job status could not be checked, false if the status could be checked and the
         * job is found to be inactive (aborted, cancelled, etc.)
         */
        private boolean performJobStatusCheck(TaskMessage tm) throws InvalidJobTaskIdException
        {
            Objects.requireNonNull(tm);
            TrackingInfo tracking = tm.getTracking();
            Objects.requireNonNull(tracking);

            String statusCheckUrl = tracking.getStatusCheckUrl();
            if (statusCheckUrl == null) {
                //If statusCheckUrl is null then we can't perform the status check so we have to assume the job is active.
                return true;
            }

            String jobId = tracking.getJobId();
            LOG.debug("Task {} (job {}) - attempting to check job status", tm.getTaskId(), jobId);
            JobStatusResponse jobStatus = getJobStatus(jobId, statusCheckUrl);
            long newStatusCheckTime = System.currentTimeMillis() + jobStatus.getStatusCheckIntervalMillis();
            LOG.debug("Task {} (job {}) - updating status check time from {} to {}", tm.getTaskId(), jobId, tracking.getStatusCheckTime(), new Date(newStatusCheckTime));
            tracking.setStatusCheckTime(new Date(newStatusCheckTime));
            return jobStatus.isActive();
        }

        /**
         * Makes a call to the status check URL to determine whether the job is active. This should make implicit use of
         * JobStatusResponseCache.
         *
         * @param jobId checks the active status of this job
         * @param statusCheckUrl full path that can be used to check job status
         * @return job status response including active status of job - true if the job is active or if the check could not be performed,
         * false if the job is inactive
         */
        private JobStatusResponse getJobStatus(String jobId, String statusCheckUrl)
        {
            JobStatusResponse jobStatusResponse = new JobStatusResponse();
            try {
                URL url = new URL(statusCheckUrl);
                URLConnection connection = url.openConnection();
                long statusCheckIntervalMillis = JobStatusResponseCache.getStatusCheckIntervalMillis(connection);
                try (BufferedReader response = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String responseValue;
                    if ((responseValue = response.readLine()) != null) {
                        LOG.debug("Job {} : retrieved active status '{}' from status check URL {}.", jobId, responseValue, statusCheckUrl);
                        jobStatusResponse.setActive(Boolean.parseBoolean(responseValue));
                    } else {
                        LOG.warn("Job {} : assuming that job is active - no suitable response from status check URL {}.", jobId, statusCheckUrl);
                        jobStatusResponse.setActive(true);
                    }
                } catch (Exception ex) {
                    LOG.warn("Job {} : assuming that job is active - failed to perform status check using URL {}. ", jobId, statusCheckUrl, ex);
                    jobStatusResponse.setActive(true);
                }

                jobStatusResponse.setStatusCheckIntervalMillis(statusCheckIntervalMillis);
            } catch (Exception e) {
                LOG.warn("Job {} : assuming that job is active - failed to perform status check using URL {}. ", jobId, statusCheckUrl, e);
                jobStatusResponse.setActive(true);
            }
            return jobStatusResponse;
        }

        private static class JobStatusResponse
        {
            private boolean isActive;
            private long statusCheckIntervalMillis;

            public JobStatusResponse()
            {
                this(true, JobStatusResponseCache.getDefaultJobStatusCheckIntervalMillis());
            }

            public JobStatusResponse(boolean isActive, long statusCheckInterval)
            {
                this.isActive = isActive;
                this.statusCheckIntervalMillis = statusCheckInterval;
            }

            public boolean isActive()
            {
                return isActive;
            }

            public void setActive(boolean active)
            {
                isActive = active;
            }

            public long getStatusCheckIntervalMillis()
            {
                return statusCheckIntervalMillis;
            }

            public void setStatusCheckIntervalMillis(long statusCheckIntervalMillis)
            {
                this.statusCheckIntervalMillis = statusCheckIntervalMillis;
            }
        }
    }

    /**
     * Called by a WorkerWrapper to indicate a task was completed by a worker.
     */
    private static class CoreWorkerCallback implements WorkerCallback
    {
        private final Codec codec;
        private final ManagedWorkerQueue workerQueue;
        private final WorkerStats stats;
        private final HealthCheckRegistry healthCheckRegistry;
        private final TransientHealthCheck transientHealthCheck;

        public CoreWorkerCallback(final Codec codec, final ManagedWorkerQueue workerQueue, final WorkerStats stats, final HealthCheckRegistry healthCheckRegistry, final TransientHealthCheck transientHealthCheck)
        {
            this.codec = Objects.requireNonNull(codec);
            this.workerQueue = Objects.requireNonNull(workerQueue);
            this.stats = Objects.requireNonNull(stats);
            this.healthCheckRegistry = Objects.requireNonNull(healthCheckRegistry);
            this.transientHealthCheck = Objects.requireNonNull(transientHealthCheck);
        }

        @Override
        public void send(final String queueMsgId, final TaskMessage responseMessage)
        {
            Objects.requireNonNull(queueMsgId);
            Objects.requireNonNull(responseMessage);
            LOG.debug("Sending task {} complete (message id: {})", responseMessage.getTaskId(), queueMsgId);

            final String queue = responseMessage.getTo();
            checkForTrackingTermination(queueMsgId, queue, responseMessage);

            final byte[] output;
            try {
                output = codec.serialise(responseMessage);
            } catch (final CodecException ex) {
                throw new RuntimeException(ex);
            }

            final int priority = responseMessage.getPriority() == null ? 0 : responseMessage.getPriority();

            try {
                workerQueue.publish("-1", output, queue, Collections.emptyMap(), priority);
            } catch (final QueueException ex) {
                throw new RuntimeException(ex);
            }
        }

        /**
         * {@inheritDoc}
         *
         * Hand off the serialised result from a worker with its status to the queue. If the result cannot be serialised for any reason,
         * we reject the task.
         */
        @Override
        public void complete(final String queueMsgId, final String queue, final TaskMessage responseMessage)
        {
            Objects.requireNonNull(queueMsgId);
            Objects.requireNonNull(responseMessage);
            // queue can be null for a dead end worker
            LOG.debug("Task {} complete (message id: {})", responseMessage.getTaskId(), queueMsgId);
            LOG.debug("Setting destination {} in task {} (message id: {})", queue, responseMessage.getTaskId(), queueMsgId);
            responseMessage.setTo(queue);
            checkForTrackingTermination(queueMsgId, queue, responseMessage);
            try {
                if (null == queue) {
                    // **** Dead End Worker ****
                    // If targetQueue is not set i.e. is null for a dead end worker. There remains a
                    // need to acknowledge the message is processed and removed from the queue. This
                    // is how a dead end worker will operate.

                    // **** Only Output Errors Worker ****
                    // If a worker is designed to output only error messages the targetQueue will be
                    // null for success messages and set to the workers output queue for error
                    // messages.
                    workerQueue.acknowledgeTask(queueMsgId);
                } else {
                    // **** Normal Worker ****                    
                    // A worker with an input and output queue.
                    byte[] output = codec.serialise(responseMessage);
                    workerQueue.publish(queueMsgId, output, queue, Collections.emptyMap(), responseMessage.getPriority() == null ? 0 : responseMessage.getPriority());
                    stats.getOutputSizes().update(output.length);
                }
                stats.updatedLastTaskFinishedTime();
                if (TaskStatus.isSuccessfulResponse(responseMessage.getTaskStatus())) {
                    stats.incrementTasksSucceeded();
                } else {
                    stats.incrementTasksFailed();
                }
            } catch (CodecException | QueueException e) {
                LOG.error("Cannot publish data for task {}, rejecting", responseMessage.getTaskId(), e);
                abandon(queueMsgId, e);
            }
        }

        @Override
        public void abandon(final String queueMsgId, final Exception e)
        {
            LOG.debug("Rejecting message id {}", queueMsgId);
            workerQueue.rejectTask(queueMsgId);
            stats.incrementTasksRejected();
            workerQueue.disconnectIncoming();
            transientHealthCheck.addTransientExceptionToRegistry(e.getMessage());
            healthCheckRegistry.runHealthCheck("transient");
        }

        @Override
        public void forward(String queueMsgId, String queue, TaskMessage forwardedMessage, Map<String, Object> headers)
        {
            Objects.requireNonNull(queueMsgId);
            Objects.requireNonNull(forwardedMessage);
            // queue can be null for a dead end worker
            LOG.debug("Task {} (message id: {}) being forwarded to queue {}", forwardedMessage.getTaskId(), queueMsgId, queue);
            checkForTrackingTermination(queueMsgId, queue, forwardedMessage);
            try {
                // If the queue is null, acknowledge the task rather than forwarding it
                if (queue == null) {
                    workerQueue.acknowledgeTask(queueMsgId);
                } else {
                    // Else forward the task
                    byte[] output = codec.serialise(forwardedMessage);
                    workerQueue.publish(queueMsgId, output, queue, headers, forwardedMessage.getPriority() == null ? 0 : forwardedMessage.getPriority());
                    stats.incrementTasksForwarded();
                    //TODO - I'm guessing this stat should not be updated for forwarded messages:
                    // stats.getOutputSizes().update(output.length);
                }
            } catch (CodecException | QueueException e) {
                LOG.error("Cannot publish data for forwarded task {}, rejecting", forwardedMessage.getTaskId(), e);
                abandon(queueMsgId, e);
            }
        }

        @Override
        public void discard(String queueMsgId)
        {
            Objects.requireNonNull(queueMsgId);
            LOG.debug("Discarding message id {}", queueMsgId);
            workerQueue.discardTask(queueMsgId);
            stats.incrementTasksDiscarded();
        }

        @Override
        public void reportUpdate(final String queueMsgId, final TaskMessage reportUpdateMessage)
        {
            Objects.requireNonNull(queueMsgId);
            Objects.requireNonNull(reportUpdateMessage);
            LOG.debug("Sending report updates to queue {})", reportUpdateMessage.getTo());

            final byte[] output;
            try {
                output = codec.serialise(reportUpdateMessage);
            } catch (final CodecException ex) {
                throw new RuntimeException(ex);
            }

            final int priority = reportUpdateMessage.getPriority() == null ? 0 : reportUpdateMessage.getPriority();

            try {
                workerQueue.publish("-1", output, reportUpdateMessage.getTo(), Collections.emptyMap(), priority);
            } catch (final QueueException ex) {
                throw new RuntimeException(ex);
            }
        }

        /**
         * Checks whether tracking of this task message should end when publishing to the specified queue. If tracking is to end then this
         * method removes and returns the tracking info from the task message.
         *
         * @param queueMsgId the reference to the message this task arrived on
         * @param queueToSend the queue to which the message is to be published
         * @param tm task message whose tracking info is to be checked
         * @return if tracking of the message terminates on publishing to the specified queue then the removed tracking info is returned;
         * otherwise null is returned and the tracking info is not removed from the message
         */
        private TrackingInfo checkForTrackingTermination(final String queueMsgId, final String queueToSend, TaskMessage tm)
        {
            Objects.requireNonNull(queueMsgId);
            Objects.requireNonNull(tm);
            // queueToSend can be null for a dead end worker

            final TrackingInfo tracking = tm.getTracking();
            if (tracking != null) {
                final String trackTo = tracking.getTrackTo();
                if ((trackTo == null && queueToSend == null) || (trackTo != null && trackTo.equalsIgnoreCase(queueToSend))) {
                    LOG.debug("Task {} (message id: {}): removing tracking info from this message as tracking ends on publishing to the queue {}.", tm.getTaskId(), queueMsgId, queueToSend);
                    tm.setTracking(null);
                }
            }
            return tracking;
        }
    }
}
