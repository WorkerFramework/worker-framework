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
package com.hpe.caf.api.worker;

public class TaskInformation {
    private final String inboundMessageId;
    private final Object responseCountLock;
    private volatile int responseCount;
    private volatile boolean isResponseCountFinal;
    private final Object acknowledgementCountLock;
    private volatile int acknowledgementCount;


    public TaskInformation(final String inboundMessageId) {
        this.inboundMessageId = inboundMessageId;
        this.responseCountLock = new Object();
        this.responseCount = 0;
        this.isResponseCountFinal = false;
        this.acknowledgementCountLock = new Object();
        this.acknowledgementCount = 0;
    }

    public String getInboundMessageId() {
        return inboundMessageId;
    }

    public void incrementResponseCount(final boolean isFinalResponse) {
        synchronized (responseCountLock) {
            if (isResponseCountFinal) {
                throw new RuntimeException("Final response already set!");
            }

            responseCount++;

            if (isFinalResponse) {
                isResponseCountFinal = true;
            }
        }
    }

    /**
     * Indicate that there are no more responses to come.
     *
     * @return true if all the responses have already been acknowledged
     */
    public boolean finalizeResponseCount()
    {
        synchronized (acknowledgementCountLock) {
            synchronized (responseCountLock) {
                isResponseCountFinal = true;
            }

            return responseCount == acknowledgementCount;
        }
    }

    /**
     * Increment the count of acknowledgements.
     *
     * @return true if this increment means that all responses have been acknowledged
     */
    public boolean incrementAcknowledgementCount()
    {
        synchronized (acknowledgementCountLock) {
            final int ackCount = ++acknowledgementCount;

            return isResponseCountFinal
                    && (responseCount == ackCount);
        }
    }

}
