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
({
    workerName: getenv("CAF_WORKER_NAME") || "worker-test",
    workerVersion: "1.0.0",
    outputQueue: getenv("CAF_WORKER_OUTPUT_QUEUE") || "testworker-out",
    rejectQueue: getenv("CAF_WORKER_REJECT_QUEUE") || "testworker-reject",
    threads: getenv("CAF_WORKER_THREADS") || 1
});