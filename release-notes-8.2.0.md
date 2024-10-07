!not-ready-for-release!

#### Version Number
${version-number}

#### New Features
- US969005: RabbitMQ password config update.  
  The `worker-default-configs` module now checks for the RabbitMQ password in the following order:
  - First, it looks for a password in the `CAF_RABBITMQ_PASSWORD` environment variable
  - If that's empty, it tries to read the password from a file specified by `CAF_RABBITMQ_PASSWORD_FILE`
  - If both options above are empty, it falls back to using the default password: `guest`

#### Known Issues
