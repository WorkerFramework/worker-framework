!not-ready-for-release!

#### Version Number
${version-number}

#### New Features

#### Known Issues

#### Breaking Changes
*   SCMOD-9988: Lyra client dropped  
    * The Lyra client used to create an auto-recovering connection to RabbitMQ has been replaced with the latest RabbitMQ 
      Java client.  
    * Registering as a `net.jodah.lyra.event.ConnectionListener` is no longer supported.
      Instead, implement the `com.rabbitmq.client.RecoveryListener` and `com.rabbitmq.client.ExceptionHandler` interfaces as 
      needed.
    * The following methods have been removed from RabbitUtil: 
        * `createRabbitConnection(ConnectionOptions opts, Config config)` 
        * `createLyraConnectionOptions(String host, int port, String user, String pass)`
        * `createLyraConfig(int backoffInterval, int maxBackoffInterval, int maxAttempts)`  

