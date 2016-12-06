/*
 * (c) Copyright 2015-2016 Hewlett Packard Enterprise Development LP
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hpe.caf.worker.testing;

import com.hpe.caf.api.Codec;
import com.hpe.caf.api.CodecException;
import com.hpe.caf.api.worker.TaskMessage;
import com.hpe.caf.api.worker.TaskStatus;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The base implementation of {@link ResultProcessor}.
 *
 * @param <TResult>   the type parameter
 * @param <TInput>    the type parameter
 * @param <TExpected> the type parameter
 */
public abstract class AbstractResultProcessor<TResult, TInput, TExpected> implements ResultProcessor {

    private final Codec codec;
    private final Class<TResult> resultClass;

    /**
     * Getter for property 'codec'.
     *
     * @return Value for property 'codec'.
     */
    protected Codec getCodec() {
        return codec;
    }

    /**
     * Instantiates a new Abstract result processor.
     *
     * @param codec       the codec
     * @param resultClass the result class
     */
    protected AbstractResultProcessor(final Codec codec, final Class<TResult> resultClass) {

        this.codec = codec;
        this.resultClass = resultClass;
    }

    @Override
    public boolean process(TestItem testItem, TaskMessage resultMessage) throws Exception {
        TResult workerResult = deserializeMessage(resultMessage, resultClass);
        return processWorkerResult(testItem, resultMessage, workerResult);
    }

    /**
     * Deserialize message to the worker-under-test result using configured {@link Codec} implementation.
     *
     * @param message     the message
     * @param resultClass the result class
     * @return the t result
     * @throws CodecException the codec exception
     */
    protected TResult deserializeMessage(TaskMessage message, Class<TResult> resultClass) throws CodecException {
        if (message.getTaskStatus() != TaskStatus.RESULT_SUCCESS && message.getTaskStatus() != TaskStatus.RESULT_FAILURE) {
            throw new AssertionError("Task status was failure.");
        }
        TResult workerResult = codec.deserialise(message.getTaskData(), resultClass);
        return workerResult;
    }

    /**
     * Processes deserialized worker-under-test result.
     *
     * @param testItem the test item
     * @param message  the message
     * @param result   the result
     * @return the boolean
     * @throws Exception the exception
     */
    protected abstract boolean processWorkerResult(TestItem<TInput, TExpected> testItem, TaskMessage message, TResult result) throws Exception;

    protected abstract boolean processFailedWorkerResult(TestItem<TInput, TExpected> testItem, TaskMessage message, Map<String, Object> map) throws Exception;

    public String getInputIdentifier(TaskMessage message) {return "";}
}
