#!/bin/bash
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

cd /maven
exec java -cp "*" com.hpe.caf.worker.core.WorkerApplication server ${dropwizardConfig}
