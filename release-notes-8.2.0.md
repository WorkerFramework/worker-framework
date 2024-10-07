!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
- US969005: RabbitMQ password config update.  
  worker-default-configs has been updated to try to read the RabbitMQ password from the file pointed to by the `CAF_RABBITMQ_PASSWORD_FILE` 
  environment variable if the `CAF_RABBITMQ_PASSWORD` environment variable is null. If both of these environment variables are null, then
  the default `guest` password will be used.

#### Known Issues
