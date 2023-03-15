/*
 * Copyright 2015-2023 Open Text.
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
package com.hpe.caf.worker.queue.rabbit;

import com.hpe.caf.api.ConfigurationException;
import com.hpe.caf.api.ConfigurationSource;
import com.hpe.caf.api.worker.MessagePriorityManager;
import com.hpe.caf.api.worker.MessagePriorityManagerProvider;

/**
 * An implementation of {@link MessagePriorityManagerProvider} which, depending on configuration, creates a priority manager which
 * increases a message priority on each response ({@link IncreasingMessagePriorityManager}) or a priority manager which always returns
 * zero ({@link ZeroMessagePriorityManager}).
 */
public class MessagePriorityManagerProviderImpl implements MessagePriorityManagerProvider
{
    @Override
    public MessagePriorityManager getMessagePriorityManager(ConfigurationSource configurationSource) throws ConfigurationException
    {
        RabbitWorkerQueueConfiguration configuration = configurationSource.getConfiguration(RabbitWorkerQueueConfiguration.class);
        if (configuration.getMaxPriority() > 0) {
            //TODO: Do we need more control? Do we want to support different behaviours?
            return new IncreasingMessagePriorityManager();
        }
        return new ZeroMessagePriorityManager();
    }
}
