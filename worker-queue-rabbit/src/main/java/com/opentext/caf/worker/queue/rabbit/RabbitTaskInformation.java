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
package com.opentext.caf.worker.queue.rabbit;

import com.opentext.caf.api.worker.TaskInformation;
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

    public RabbitTaskInformation(final String inboundMessageId) {
        this(inboundMessageId, false);
    }
    
    public RabbitTaskInformation(final String inboundMessageId, final boolean isPoison) {
        this.inboundMessageId = inboundMessageId;
        this.responseCount = new AtomicInteger(0);
        this.isResponseCountFinal = new AtomicBoolean(false);
        this.acknowledgementCount = new AtomicInteger(0);
        this.negativeAckEventSent = new AtomicBoolean(false);
        this.ackEventSent = new AtomicBoolean(false);
        this.isPoison = isPoison;
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
}
