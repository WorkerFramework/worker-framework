/*
 * Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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
package com.hpe.caf.util.rabbitmq;

/**
 * CAF RabbitMQ headers
 */
public class RabbitHeaders
{
    public static final String RABBIT_HEADER_CAF_WORKER_REJECTED = "x-caf-worker-rejected";
    public static final String RABBIT_HEADER_CAF_WORKER_RETRY = "x-caf-worker-retry";
    public static final String RABBIT_HEADER_CAF_WORKER_RETRY_LIMIT = "x-caf-worker-retry-limit";
}
