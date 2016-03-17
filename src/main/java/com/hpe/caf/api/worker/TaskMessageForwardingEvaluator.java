package com.hpe.caf.api.worker;

/**
 * A worker should implement this interface if it is capable of evaluating task
 * messages and deciding whether they are to be forwarded rather than executed.
 */
public interface TaskMessageForwardingEvaluator {
    /**
     * Examines the task message and decides whether to forward it or take some other action, e.g. discard.
     * @param tm the task message
     * @param queueMessageId the reference to the message this task arrived on
     * @param callback worker callback to enact the forwarding action determined by the worker
     */
    void determineForwardingAction(TaskMessage tm, String queueMessageId, WorkerCallback callback);
}
