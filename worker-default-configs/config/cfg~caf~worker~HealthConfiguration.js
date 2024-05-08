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
({
    livenessInitialDelaySeconds: getenv("CAF_LIVENESS_INITIAL_DELAY_SECONDS") || 15,
    livenessCheckIntervalSeconds: getenv("CAF_LIVENESS_CHECK_INTERVAL_SECONDS") || 60,
    livenessDowntimeIntervalSeconds: getenv("CAF_LIVENESS_DOWNTIME_INTERVAL_SECONDS") || 60,
    livenessSuccessAttempts: getenv("CAF_LIVENESS_SUCCESS_ATTEMPTS") || 1,
    livenessFailureAttempts: getenv("CAF_LIVENESS_FAILURE_ATTEMPTS") || 3,
    readinessInitialDelaySeconds: getenv("CAF_READINESS_INITIAL_DELAY_SECONDS") || 15,
    readinessCheckIntervalSeconds: getenv("CAF_READINESS_CHECK_INTERVAL_SECONDS") || 60,
    readinessDowntimeIntervalSeconds: getenv("CAF_READINESS_DOWNTIME_INTERVAL_SECONDS") || 60,
    readinessSuccessAttempts: getenv("CAF_READINESS_SUCCESS_ATTEMPTS") || 1,
    readinessFailureAttempts: getenv("CAF_READINESS_FAILURE_ATTEMPTS") || 3
});
