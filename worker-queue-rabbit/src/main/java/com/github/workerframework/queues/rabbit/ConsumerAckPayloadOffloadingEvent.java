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

import com.github.workerframework.api.DataStore;
import com.github.workerframework.api.DataStoreException;
import com.github.workerframework.util.rabbitmq.ConsumerAckEvent;
import com.github.workerframework.util.rabbitmq.QueueConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A customized ConsumerAckEvent that deletes the payload from the DataStore after the message has been acknowledged.
 */
public class ConsumerAckPayloadOffloadingEvent extends ConsumerAckEvent
{
    private static final Logger LOG = LoggerFactory.getLogger(ConsumerAckPayloadOffloadingEvent.class);
    
    private final DataStore dataStore;
    private final String dataStoreRef;
    
    public ConsumerAckPayloadOffloadingEvent(final long tag, final DataStore dataStore, final String dataStoreRef)
    {
        super(tag);
        this.dataStore = dataStore;
        this.dataStoreRef = dataStoreRef;
    }
    
    @Override
    public void handleEvent(final QueueConsumer target)
    {
        super.handleEvent(target);
        try {
            dataStore.delete(dataStoreRef);
        } catch (final DataStoreException e) {
            LOG.error("Failed to delete an offloaded message:{} from the datastore", dataStoreRef, e);
        }
    }
}
