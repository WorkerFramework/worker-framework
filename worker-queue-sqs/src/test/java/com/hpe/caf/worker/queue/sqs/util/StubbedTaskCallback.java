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
package com.hpe.caf.worker.queue.sqs.util;

import com.hpe.caf.api.worker.InvalidTaskException;
import com.hpe.caf.api.worker.TaskCallback;
import com.hpe.caf.api.worker.TaskInformation;
import com.hpe.caf.api.worker.TaskRejectedException;
import com.hpe.caf.worker.queue.sqs.SQSTaskInformation;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class StubbedTaskCallback implements TaskCallback
{
    private final BlockingQueue<CallbackResponse> callbackQueue = new LinkedBlockingQueue<>();
    private final BlockingQueue<CallbackResponse> callbackDLQ = new LinkedBlockingQueue<>();

    @Override
    public void registerNewTask(
            final TaskInformation taskInformation,
            final byte[] taskData,
            final Map<String, Object> headers
    ) throws TaskRejectedException, InvalidTaskException
    {
        var sqsTaskInformation = (SQSTaskInformation) taskInformation;
        final var body = new String(taskData);
        if (body.equals("REJECTED")) {
            throw new TaskRejectedException("REJECTED");
        }

        if (body.equals("INVALID")) {
            throw new InvalidTaskException("INVALID");
        }
        final var response = new CallbackResponse(sqsTaskInformation, new String(taskData), headers);
        if (sqsTaskInformation.getQueueInfo().name().endsWith(SQSUtil.DEAD_LETTER_QUEUE_SUFFIX)) {
            callbackDLQ.add(response);
        } else {
            callbackQueue.add(response);
        }
    }

    @Override
    public void abortTasks()
    {

    }

    public BlockingQueue<CallbackResponse> getCallbackQueue()
    {
        return callbackQueue;
    }

    public BlockingQueue<CallbackResponse> getCallbackDLQ()
    {
        return callbackDLQ;
    }
}
