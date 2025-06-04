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

public class TransientDeliveryException extends Exception {
    private static final long serialVersionUID = 1L;
    private final long messageId;

    /**
     * Create a new TransientDeliveryException with the specified message.
     *
     * @param message the message to include in the exception
     */
    public TransientDeliveryException(final String message, final long messageId, final Throwable cause) {
        super(message);
        this.messageId = messageId;
    }

    public long getMessageId() {
        return messageId;
    }
}
