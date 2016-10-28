package com.hpe.caf.api.worker;


/**
 * Provides metrics for a WorkerQueue.
 * @since 4.0
 */
public interface WorkerQueueMetricsReporter
{
    /**
     * @return number of failures/errors encountered by the WorkerQueue so far
     */
    int getQueueErrors();


    /**
     * @return the number of messages received by the WorkerQueue so far
     */
    int getMessagesReceived();


    /**
     * @return the number of messages published by the WorkerQueue so far
     */
    int getMessagesPublished();


    /**
     * @return the number of messages that have been rejected/requeued by the WorkerQueue so far
     */
    int getMessagesRejected();


    /**
     * @return the number of messages that have been dropped by the WorkerQueue so far
     */
    int getMessagesDropped();
}
