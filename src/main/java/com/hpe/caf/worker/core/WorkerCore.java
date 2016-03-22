package com.hpe.caf.worker.core;


import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.DecodeMethod;
import com.hpe.caf.api.worker.*;
import com.hpe.caf.naming.ServicePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;


/**
 * WorkerCore represents the main logic of the microservice worker. It is responsible for accepting
 * new tasks from a WorkerQueue, handing them off to a backend Worker and executing them upon a thread pool.
 * It will then accept a result from the Worker it executed and hand the TaskResult back to the WorkerQueue
 * for publishing.
 */
public class WorkerCore
{
    private final ThreadPoolExecutor threadPool;
    private final ManagedWorkerQueue workerQueue;
    private final WorkerStats stats = new WorkerStats();
    private final TaskCallback callback;
    private final ConcurrentMap<String, Future<?>> tasks = new ConcurrentHashMap<>();
    private static final Logger LOG = LoggerFactory.getLogger(WorkerCore.class);


    public WorkerCore(final Codec codec, final ThreadPoolExecutor pool, final ManagedWorkerQueue queue, final WorkerFactory factory, final ServicePath path)
    {
        WorkerCallback taskCallback =  new CoreWorkerCallback(codec, queue, stats, tasks);
        this.threadPool = Objects.requireNonNull(pool);
        this.callback = new CoreTaskCallback(codec, stats, new WorkerExecutor(path, taskCallback, factory, tasks, threadPool), tasks, queue);
        this.workerQueue = Objects.requireNonNull(queue);
    }


    /**
     * Begin accepting tasks to process.
     * @throws QueueException if the queues cannot be started
     */
    public void start()
        throws QueueException
    {
        workerQueue.start(callback);
    }


    /**
     * The current idle time for the worker. If there are any active threads, this is 0. Otherwise it is the
     * difference between the current time and completion time of the last completed task.
     * @return the current idle time in milliseconds
     */
    public long getCurrentIdleTime()
    {
        if ( threadPool.getActiveCount() == 0 ) {
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
        return threadPool.getQueue().size();
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
        private final Map<String, Future<?>> taskMap;
        private final ManagedWorkerQueue workerQueue;
        private final Cache<String, JobStatusResponse> jobStatusCache;
        private static final String DEFAULT_JOB_STATUS_CACHE_ITEM_LIFETIME_SECS = "300";
        private static final String CAF_JOB_STATUS_CACHE_ITEM_LIFETIME_SECS_VAR_NAME = "CAF_JOB_STATUS_CACHE_ITEM_LIFETIME_SECS";


        public CoreTaskCallback(final Codec codec, final WorkerStats stats, final WorkerExecutor executor, final Map<String, Future<?>> tasks, final ManagedWorkerQueue workerQueue)
        {
            this.codec = Objects.requireNonNull(codec);
            this.stats = Objects.requireNonNull(stats);
            this.executor = Objects.requireNonNull(executor);
            this.taskMap = Objects.requireNonNull(tasks);
            this.workerQueue = Objects.requireNonNull(workerQueue);
            this.jobStatusCache = CacheBuilder.newBuilder()
                    .maximumSize(1000)
                    .expireAfterWrite(getJobStatusCacheItemLifetimeSecs(), TimeUnit.SECONDS)
                    .build();
        }


        /**
         * {@inheritDoc}
         *
         * Use the factory to get a new worker to handle the task, wrap this in a handler
         * and hand it off to the thread pool.
         */
        @Override
        public void registerNewTask(final String queueMsgId, final byte[] taskMessage, Map<String, Object> headers)
                throws InvalidTaskException, TaskRejectedException
        {
            Objects.requireNonNull(queueMsgId);
            try {
                stats.incrementTasksReceived();
                stats.getInputSizes().update(taskMessage.length);
                TaskMessage tm = codec.deserialise(taskMessage, TaskMessage.class, DecodeMethod.LENIENT);
                LOG.debug("Received task {} (message id: {})", tm.getTaskId(), queueMsgId);
                checkForTrackingTermination(queueMsgId, tm);
                boolean taskIsActive = checkStatus(tm);
                if (taskIsActive) {
                    if (tm.getTo() == null || tm.getTo().equalsIgnoreCase(workerQueue.getInputQueue())) {
                        LOG.debug("Task {} (message id: {}) on input queue {} {}", tm.getTaskId(), queueMsgId, workerQueue.getInputQueue(), (tm.getTo() != null) ? "is intended for this worker" : "has no explicit destination, therefore assuming it is intended for this worker");
                        executor.executeTask(tm, queueMsgId);
                    } else {
                        LOG.debug("Task {} (message id: {}) is not intended for this worker: input queue {} does not match message destination queue {}", tm.getTaskId(), queueMsgId, workerQueue.getInputQueue(), tm.getTo());
                        executor.forwardTask(tm, queueMsgId, headers);
                    }
                } else {
                    LOG.debug("Task {} is no longer active. The task message (message id: {}) will not be executed", tm.getTaskId(), queueMsgId);
                    executor.discardTask(tm, queueMsgId);
                }
            } catch (CodecException e) {
                stats.incrementTasksRejected();
                throw new InvalidTaskException("Queue data did not deserialise to a TaskMessage", e);
            } catch (InvalidJobTaskIdException ijte) {
                stats.incrementTasksRejected();
                throw new InvalidTaskException("TaskMessage contains an invalid job task identifier", ijte);
            }
        }


        /**
         * Cancel all the Future objects in our Map of running tasks. If the task is not yet
         * running it will just be thrown out of the queue. If it has completed this has no
         * effect. If it is running the Thread will be interrupted.
         */
        @Override
        public void abortTasks()
        {
            LOG.warn("Aborting all current queued and in-progress tasks");
            taskMap.forEach((key, value) -> {
                value.cancel(true);
                stats.incrementTasksAborted();
            });
            taskMap.clear();
        }


        /**
         * @return the lifetime, in seconds, of cached job status values
         */
        private int getJobStatusCacheItemLifetimeSecs()
        {
            String lifetime = System.getProperty(CAF_JOB_STATUS_CACHE_ITEM_LIFETIME_SECS_VAR_NAME);
            if (lifetime == null) {
                LOG.debug("{} system property not found", CAF_JOB_STATUS_CACHE_ITEM_LIFETIME_SECS_VAR_NAME);
                lifetime = System.getenv(CAF_JOB_STATUS_CACHE_ITEM_LIFETIME_SECS_VAR_NAME);
                if (lifetime == null) {
                    LOG.debug("{} environment variable not found - using default value {}", CAF_JOB_STATUS_CACHE_ITEM_LIFETIME_SECS_VAR_NAME, DEFAULT_JOB_STATUS_CACHE_ITEM_LIFETIME_SECS);
                    lifetime = DEFAULT_JOB_STATUS_CACHE_ITEM_LIFETIME_SECS;
                }
            }
            return Integer.parseInt(lifetime);
        }


        /**
         * Checks whether a task is still active.
         * If a status check cannot be performed then the task is assumed to be active.
         * Checking status may result in a change to the tracking info on the supplied task message.
         * @param tm task message to be checked to verify whether the task is still active
         * @return true if the task is still active, false otherwise
         */
        private boolean checkStatus(TaskMessage tm) throws InvalidJobTaskIdException {
            Objects.requireNonNull(tm);

            TrackingInfo tracking = tm.getTracking();
            if (tracking != null) {
                Date statusCheckTime = tracking.getStatusCheckTime();
                Objects.requireNonNull(statusCheckTime);
                if (statusCheckTime.getTime() <= System.currentTimeMillis()) {
                    return performJobStatusCheck(tm);
                }
                LOG.debug("Task {} active status is not being checked - it is not yet time for the status check to be performed", tm.getTaskId());
            } else {
                LOG.debug("Task {} active status is not being checked - the task message does not have tracking info", tm.getTaskId());
            }

            //By default a task is considered to be active.
            return true;
        }


        /**
         * Checks the current active status of the job to which the task belongs.
         * If this check can be made successfully then the status check time of the supplied task message is updated.
         * @param tm the task message whose job status will be verified
         * @return true if the task's job is active or the job status could not be checked, false if the status could be checked and the job is found to be inactive (aborted, cancelled, etc.)
         */
        private boolean performJobStatusCheck(TaskMessage tm) throws InvalidJobTaskIdException {
            Objects.requireNonNull(tm);
            TrackingInfo tracking = tm.getTracking();
            Objects.requireNonNull(tracking);
            String statusCheckUrl = tracking.getStatusCheckUrl();
            Objects.requireNonNull(statusCheckUrl);
            String jobTaskId = tracking.getJobTaskId();
            Objects.requireNonNull(jobTaskId);
            String jobId = tracking.getJobId();
            JobStatusResponse jobStatus = jobStatusCache.getIfPresent(jobId);
            if (jobStatus != null) {
                LOG.debug("Task {} (job {}) - using cached job status", tm.getTaskId(), jobId);
            } else {
                LOG.debug("Task {} (job {}) - attempting to check job status", tm.getTaskId(), jobId);
                jobStatus = getJobStatus(jobId, statusCheckUrl);
                jobStatusCache.put(jobId, jobStatus);
            }
            long newStatusCheckTime = System.currentTimeMillis() + jobStatus.getStatusCheckIntervalMillis();
            tracking.setStatusCheckTime(new Date(newStatusCheckTime));
            return jobStatus.isActive();
        }


        /**
         * Makes a call to the status check URL to determine whether the job is active.
         * @param jobId checks the active status of this job
         * @param statusCheckUrl full path that can be used to check job status
         * @return job status response including active status of job - true if the job is active or if the check could not be performed, false if the job is inactive
         */
        private JobStatusResponse getJobStatus(String jobId, String statusCheckUrl) {
            JobStatusResponse jobStatusResponse = new JobStatusResponse();
            try {
                URL url = new URL(statusCheckUrl);
                URLConnection connection = url.openConnection();
                try (BufferedReader response = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                    String responseValue;
                    if ((responseValue = response.readLine()) != null) {
                        jobStatusResponse.setActive(Boolean.parseBoolean(responseValue));
                    } else {
                        LOG.warn("Job {} : assuming that job is active - no suitable response from status check URL {} : {}", jobId, statusCheckUrl);
                        jobStatusResponse.setActive(true);
                    }
                } catch (Exception ex) {
                    LOG.warn("Job {} : assuming that job is active - failed to perform status check using URL {} : {}", jobId, statusCheckUrl, ex);
                    jobStatusResponse.setActive(true);
                }

                long statusCheckIntervalMillis = 3600000; //TODO - statusCheckIntervalMillis value should come from statusCheckUrl response header - see CAF-870
                jobStatusResponse.setStatusCheckIntervalMillis(statusCheckIntervalMillis);
            } catch (Exception e) {
                LOG.warn("Job {} : assuming that job is active - failed to perform status check using URL {} : {}", jobId, statusCheckUrl, e);
                jobStatusResponse.setActive(true);
            }
            return jobStatusResponse;
        }


        /**
         * Checks whether tracking of this task message should end when arriving at the specified queue.
         * If tracking is to end here then tracking info is removed from the task message.
         * @param queueMsgId the reference to the message this task arrived on
         * @param tm task message whose tracking info is to be checked
         */
        private void checkForTrackingTermination(String queueMsgId, TaskMessage tm) {
            Objects.requireNonNull(queueMsgId);
            Objects.requireNonNull(tm);

            TrackingInfo tracking = tm.getTracking();
            if (tracking != null) {
                String trackTo = tracking.getTrackTo();
                if (trackTo != null && trackTo.equalsIgnoreCase(workerQueue.getInputQueue())) {
                    LOG.debug("Task {} (message id: {}) on input queue {}: removing tracking info from this message as tracking ends on arrival at this queue", tm.getTaskId(), queueMsgId, workerQueue.getInputQueue());
                    tm.setTracking(null);
                }
            }
        }


        private static class JobStatusResponse {
            private static final long defaultJobStatusCheckIntervalMillis = 120000;

            private boolean isActive;
            private long statusCheckIntervalMillis;

            public JobStatusResponse() {
                this(true, defaultJobStatusCheckIntervalMillis);
            }

            public JobStatusResponse(boolean isActive, long statusCheckInterval) {
                this.isActive = isActive;
                this.statusCheckIntervalMillis = statusCheckInterval;
            }

            public boolean isActive() {
                return isActive;
            }

            public void setActive(boolean active) {
                isActive = active;
            }

            public long getStatusCheckIntervalMillis() {
                return statusCheckIntervalMillis;
            }

            public void setStatusCheckIntervalMillis(long statusCheckIntervalMillis) {
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
        private final WorkerQueue workerQueue;
        private final WorkerStats stats;
        private final ConcurrentMap<String, Future<?>> taskMap;


        public CoreWorkerCallback(final Codec codec, final WorkerQueue queue, final WorkerStats stats, final ConcurrentMap<String, Future<?>> tasks)
        {
            this.codec = Objects.requireNonNull(codec);
            this.workerQueue = Objects.requireNonNull(queue);
            this.stats = Objects.requireNonNull(stats);
            this.taskMap = Objects.requireNonNull(tasks);
        }


        /**
         * {@inheritDoc}
         *
         * Hand off the serialised result from a worker with its status to the queue. If the result cannot
         * be serialised for any reason, we reject the task.
         */
        @Override
        public void complete(final String queueMsgId, final String queue, final TaskMessage responseMessage)
        {
            Objects.requireNonNull(queueMsgId);
            Objects.requireNonNull(queue);
            Objects.requireNonNull(responseMessage);
            taskMap.remove(queueMsgId);
            LOG.debug("Task {} complete (message id: {})", responseMessage.getTaskId(), queueMsgId);
            LOG.debug("Setting destination {} in task {} (message id: {})", queue, responseMessage.getTaskId(), queueMsgId);
            responseMessage.setTo(queue);
            try {
                byte[] output = codec.serialise(responseMessage);
                workerQueue.publish(queueMsgId, output, getTargetQueue(queueMsgId, responseMessage, queue), Collections.emptyMap());
                stats.getOutputSizes().update(output.length);
                stats.updatedLastTaskFinishedTime();
                if ( TaskStatus.isSuccessfulResponse(responseMessage.getTaskStatus()) ) {
                    stats.incrementTasksSucceeded();
                } else {
                    stats.incrementTasksFailed();
                }
            } catch (CodecException | QueueException e) {
                LOG.error("Cannot publish data for task {}, rejecting", responseMessage.getTaskId(), e);
                abandon(queueMsgId);
            }
        }


        @Override
        public void abandon(final String queueMsgId)
        {
            LOG.debug("Rejecting message id {}", queueMsgId);
            workerQueue.rejectTask(queueMsgId);
            stats.incrementTasksRejected();
        }


        @Override
        public void forward(String queueMsgId, String queue, TaskMessage forwardedMessage, Map<String, Object> headers) {
            Objects.requireNonNull(queueMsgId);
            Objects.requireNonNull(queue);
            Objects.requireNonNull(forwardedMessage);
            taskMap.remove(queueMsgId);
            LOG.debug("Task {} (message id: {}) being forwarded to queue {}", forwardedMessage.getTaskId(), queueMsgId, queue);
            try {
                byte[] output = codec.serialise(forwardedMessage);
                workerQueue.publish(queueMsgId, output, queue, headers);
                stats.incrementTasksForwarded();
                //TODO - I'm guessing this stat should not be updated for forwarded messages:
                // stats.getOutputSizes().update(output.length);
            } catch (CodecException | QueueException e) {
                LOG.error("Cannot publish data for forwarded task {}, rejecting", forwardedMessage.getTaskId(), e);
                abandon(queueMsgId);
            }
        }


        @Override
        public void discard(String queueMsgId) {
            Objects.requireNonNull(queueMsgId);
            LOG.debug("Discarding message id {}", queueMsgId);
            workerQueue.discardTask(queueMsgId);
            stats.incrementTasksDiscarded();
        }


        /**
         * Attempts to derive the target queue (the queue that the task message is to be sent out to)
         * from the tracking info on the task message. If the message has no tracking info specifying
         * a tracking destination then the default target queue is used.
         * @param queueMsgId the reference to the message this task arrived on
         * @param tm the task message being dispatched to the target queue
         * @param defaultTargetQueue dispatch the message to this queue if the message has no tracking info specifying a tracking destination
         * @return the queue to which the message should be dispatched
         */
        private String getTargetQueue(String queueMsgId, TaskMessage tm, String defaultTargetQueue) {
            Objects.requireNonNull(tm);
            TrackingInfo tracking = tm.getTracking();
            String trackingPipe = tracking == null ? null : tracking.getTrackingPipe();
            if (isInputQueue(trackingPipe)) {
                // If this worker is the tracking destination then there's no point redirecting to self!
                LOG.debug("Task {} (message id: {}) tracking pipe matches the current input queue - this worker is the tracking destination for this message", tm.getTaskId(), queueMsgId);
                return defaultTargetQueue;
            }
            return trackingPipe == null ? defaultTargetQueue : trackingPipe;
        }


        private boolean isInputQueue(final String queue) {
            return queue == null ? false : queue.equalsIgnoreCase(workerQueue.getInputQueue());
        }
    }
}
