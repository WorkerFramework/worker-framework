#!/bin/bash
#
# Copyright 2015-2024 Open Text.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

export CAF_APPNAME=caf/worker
export CAF_RESOURCE_PATH=com/github/workerframework/testworker/config
export CAF_CONFIG_DECODER=JavascriptDecoder
export CAF_CONFIG_ENABLE_SUBSTITUTOR=false

export CAF_WORKER_DISABLE_ZERO_PROGRESS_REPORTING=true

cd /maven
exec java $CAF_WORKER_JAVA_OPTS \
    -Dpolyglot.engine.WarnInterpreterOnly=false \
    -cp '*' \
    com.hpe.caf.worker.core.WorkerApplication \
    server \
    /maven/worker.yaml
