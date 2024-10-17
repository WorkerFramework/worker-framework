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
package com.opentext.caf.api.worker;

/**
 * Provides metrics for a WorkerQueue.
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
