/*
 * Copyright 2015-2021 Micro Focus or one of its affiliates.
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

import java.io.IOException;

import org.apache.commons.codec.binary.Base64;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.QueueTaskMessage;
import com.hpe.caf.api.worker.TaskMessage;

public final class QueueTaskMessageFunctions
{
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    private QueueTaskMessageFunctions()
    {
    }
    
    public static TaskMessage from(final QueueTaskMessage queueTaskMessage, final Codec codec) throws CodecException
    {
        final byte[] taskData;
        if (isTaskDataString(queueTaskMessage)) {
            taskData = Base64.decodeBase64((String)queueTaskMessage.getTaskData());
        } else {
            taskData = codec.serialise(queueTaskMessage.getTaskData());
        }
        return new TaskMessage(queueTaskMessage.getTaskId(),
                queueTaskMessage.getTaskClassifier(),
                queueTaskMessage.getTaskApiVersion(),
                taskData,
                queueTaskMessage.getTaskStatus(),
                queueTaskMessage.getContext(),
                queueTaskMessage.getTo(),
                queueTaskMessage.getTracking(),
                queueTaskMessage.getSourceInfo(),
                queueTaskMessage.getCorrelationId());
    }
    
    public static QueueTaskMessage from(final TaskMessage taskMessage) throws CodecException
    {
        final Object taskData;
        try {
            taskData = OBJECT_MAPPER.readTree(taskMessage.getTaskData());
        } catch (final IOException e) {
            throw new CodecException("Exception while converting taskData to JsonNode", e);
        }
        return new QueueTaskMessage(taskMessage.getTaskId(),
                taskMessage.getTaskClassifier(),
                taskMessage.getTaskApiVersion(),
                taskData,
                taskMessage.getTaskStatus(),
                taskMessage.getContext(),
                taskMessage.getTo(),
                taskMessage.getTracking(),
                taskMessage.getSourceInfo(),
                taskMessage.getCorrelationId());
    }
    
    public static boolean isTaskDataString(final QueueTaskMessage queueTaskMessage)
    {
        return queueTaskMessage.getTaskData() instanceof String;
    }
}
