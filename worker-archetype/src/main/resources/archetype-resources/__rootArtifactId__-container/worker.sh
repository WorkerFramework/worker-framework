#!/bin/bash
#
# Copyright 2015-2017 Hewlett Packard Enterprise Development LP.
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

dropwizardConfig="/maven/worker.yaml"

####################################################
# Sets the dropwizard config file to a path passed in by environment variable if a variable was passed in and the file exists there.
####################################################
function set_dropwizard_config_file_location_if_mounted(){
  if [ "$DROPWIZARD_CONFIG_PATH" ] && [ -e "$DROPWIZARD_CONFIG_PATH" ];
  then
    echo "Using dropwizard config file at $DROPWIZARD_CONFIG_PATH"
    dropwizardConfig="$DROPWIZARD_CONFIG_PATH"
  fi
}
set_dropwizard_config_file_location_if_mounted

function install_certificate(){
    #This will import the CA Cert from $MESOS_SANDBOX/$SSL_CA_CRT to the default Java keystore location depending on your distribution.
    /maven/container-cert-script/install-ca-cert-java.sh
}

install_certificate

# If the CAF_APPNAME and CAF_CONFIG_PATH environment variables are not set, then use the
# JavaScript-encoded config files that are built into the container
if [ -z "$CAF_APPNAME" ] && [ -z "$CAF_CONFIG_PATH" ];
then
  export CAF_APPNAME=caf/worker
  export CAF_CONFIG_PATH=/maven/config
  export CAF_CONFIG_DECODER=JavascriptDecoder
  export CAF_CONFIG_ENABLE_SUBSTITUTOR=false
fi

cd /maven
exec java -cp "*" com.hpe.caf.worker.core.WorkerApplication server ${dropwizardConfig}
