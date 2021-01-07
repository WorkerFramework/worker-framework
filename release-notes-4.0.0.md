
#### Version Number
${version-number}

#### New Features

* caf-common upgraded to [1.20.0-288](https://github.com/CAFapi/caf-common/releases/tag/v1.20.0)

#### Breaking Changes
*   [SCMOD-9988](https://portal.digitalsafe.net/browse/SCMOD-9988): Lyra client dropped  
    * The Lyra client used to create an auto-recovering connection to RabbitMQ has been replaced with the latest RabbitMQ 
      Java client.  
    * Registering as a `net.jodah.lyra.event.ConnectionListener` is no longer supported.
      Instead, implement the `com.rabbitmq.client.RecoveryListener` and `com.rabbitmq.client.ExceptionHandler` interfaces as 
      needed.
    * The following methods have been removed from RabbitUtil: 
        * `createRabbitConnection(ConnectionOptions opts, Config config)` 
        * `createLyraConnectionOptions(String host, int port, String user, String pass)`
        * `createLyraConfig(int backoffInterval, int maxBackoffInterval, int maxAttempts)`  

#### Known Issues

* None
