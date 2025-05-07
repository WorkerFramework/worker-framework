/*
 * Copyright 2015-2025 Open Text.
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
package com.github.workerframework.queues.rabbit;

import com.github.workerframework.api.TaskInformation;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RabbitTaskInformation implements TaskInformation {
    private final String inboundMessageId;
    private final AtomicBoolean negativeAckEventSent; 
    private final AtomicBoolean ackEventSent; 
    private final AtomicInteger responseCount;
    private final AtomicBoolean isResponseCountFinal;
    private final AtomicInteger acknowledgementCount;
    private static final Logger LOG = LoggerFactory.getLogger(RabbitTaskInformation.class);
    private final boolean isPoison;
    private final Optional<String> dehydratedTaskMessageStorageRef;
    private final Optional<String> taskMessagePartialRef;

    public RabbitTaskInformation(final String inboundMessageId) {
        this(inboundMessageId, false);
    }
    
    public RabbitTaskInformation(final String inboundMessageId, final boolean isPoison) {
        this(inboundMessageId, isPoison, Optional.empty(), Optional.empty());
    }

    public RabbitTaskInformation(
        final String inboundMessageId, 
        final boolean isPoison, 
        final Optional<String> dehydratedTaskMessageStorageRef,
        final Optional<String> taskMessagePartialRef
    ) {
        this(
            inboundMessageId, 
            new AtomicInteger(0), 
            new AtomicBoolean(false),  
            new AtomicInteger(0), 
            new AtomicBoolean(false), 
            new AtomicBoolean(false), 
            isPoison,
            dehydratedTaskMessageStorageRef,
            taskMessagePartialRef
        );
    }

    public RabbitTaskInformation(
        final String inboundMessageId, 
        final AtomicInteger responseCount,
        final AtomicBoolean isResponseCountFinal,
        final AtomicInteger acknowledgementCount,
        final AtomicBoolean negativeAckEventSent,
        final AtomicBoolean ackEventSent,
        final boolean isPoison, 
        final Optional<String> dehydratedTaskMessageStorageRef,
        final Optional<String> taskMessagePartialRef
        ) {
        this.inboundMessageId = inboundMessageId;
        this.responseCount = responseCount;
        this.isResponseCountFinal = isResponseCountFinal;
        this.acknowledgementCount = acknowledgementCount;
        this.negativeAckEventSent = negativeAckEventSent;
        this.ackEventSent = ackEventSent;
        this.isPoison = isPoison;
        this.dehydratedTaskMessageStorageRef = dehydratedTaskMessageStorageRef;
        this.taskMessagePartialRef = taskMessagePartialRef;
    }

    @Override
    public String getInboundMessageId() {
        return inboundMessageId;
    }
    /**
     *
     * Increment the count of Responses.
     *
     */
    public void incrementResponseCount(final boolean isFinalResponse) {       
            if (isResponseCountFinal.get()) {
                throw new RuntimeException("Final response already set!");
            }
            responseCount.incrementAndGet();
            LOG.debug("Incremeneted the ResponseCount for message:{} Now ResCount is: {}", inboundMessageId, responseCount);
            if (isFinalResponse) {
                isResponseCountFinal.set(true);
            }        
    }

    /**
     *
     * Increment the count of acknowledgements.
     *
     */
    public void incrementAcknowledgementCount()
    {
        acknowledgementCount.incrementAndGet();
        LOG.debug("Incremeneted the AcknowledgementCount for message:{} Now AckCount is: {}", inboundMessageId, acknowledgementCount);
    }

    /**
     * Check if all response have been acknowledged
     *
     * @return true if all responses have been acknowledged and isResponseFinal is true
     */
    public boolean areAllResponsesAcknowledged()
    {        
        if (!isResponseCountFinal.get()) {
            LOG.debug("Final response count is not known yet!");
            return false;
        }
        LOG.debug("Now AckCount is: {} and ResCount is: {}", acknowledgementCount, responseCount);
        return (responseCount.intValue()==acknowledgementCount.intValue());   
    }
    
    /**
     *
     * Get the NegativeAckEventSent flag
     *
     */
    public boolean isNegativeAckEventSent()
    {
        return negativeAckEventSent.get();
    }

    /**
     *
     * Mark the NegativeAckEventSent flag to true to avoid multiple 
     * NegativeAckEvent being sent for same inboundMessageId.
     *
     */
    public void markNegativeAckEventAsSent()
    {
        negativeAckEventSent.set(true);
    }
    
    /**
     *
     * Get the AckEventSent flag
     *
     */
    public boolean isAckEventSent()
    {
        return ackEventSent.get();
    }

    /**
     *
     * Mark the ackEventSent flag to true to avoid multiple 
     * ackEventSent being sent for same inboundMessageId.
     *
     */
    public void markAckEventAsSent()
    {
        ackEventSent.set(true);
    }

    /**
     * Check if the message is poison
     *
     * @return true if the message has been marked as poisonous
     */
    public boolean isPoison() {
        return isPoison;
    }

    public Optional<String> getDehydratedTaskMessageStorageRef() {
        return dehydratedTaskMessageStorageRef;
    }

    public Optional<String> getTaskMessagePartialRef() {
        return taskMessagePartialRef;
    }
}
