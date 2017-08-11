### Common CAF Properties
#### docker-registry
- The docker repository that images will be pulled from. The template json files use this to build up the image name they tell Marathon to pull down. The  image name will start with this property value and then a name specific to the application in the json file and a version tag, an example of this is shown in the excerpt from 'marathon-workflow-policy-worker.json' below;<pre><code>"image": "${docker-registry}/policy/worker:${workflow-worker-version}",</code></pre>

#### marathon-group
- This name will be used to group together applications in Marathon, enabling the ability to make applications dependant on others. It will also be used as a prefix for queue names used with workers and to name the configuration files passed to workers. Changing this between runs of Marathon Loader would allow you to launch separate sets of the applications alongside each other that would be independent.

#### forcePullImage
- This is used to tell Marathon that it should always pull the docker image for the application rather than relying on a locally cached version of the image. This should be set to either 'true' or 'false', defaulted to 'false' in the properties file.

#### caf-fs-storage-hostPath
- If using the file system based CAF Datastore, this property specifies the path on the host machine to the folder acting as the Datastore. This should be a folder docker can reach to be able to mount the folder on each container.

#### marathon-uris-root
- The root folder that contains all files available for Marathon to copy into containers.

#### worker-config-location
- The folder within 'marathon-uris-root' that the configuration files for workers reside e.g. If the folder on disk was '/vagrant/config', 'marathon-uris-root' would be '/vagrant' and 'worker-config-location' would be 'config'. This should be the config output folder set for Marathon-loader.

#### docker-login-config
- Following the steps [here](https://mesosphere.github.io/marathon/docs/native-docker-private-registry.html) this property specifies the path to the tar containing your docker login configuration file. This is required for each container to be able to pull their image down from Artifactory. **Note this value must be replaced.**

### Rabbit properties
#### rabbit-id
- Specifies the name of the RabbitMQ Server application in Marathon. Changing this will allow the creation of distinct RabbitMQ applications.

#### rabbit-cpus:
- Configures the amount of CPU of each RabbitMQ container. This does not have to be a whole number.

#### rabbit-mem
- Configures the amount of RAM of each RabbitMQ container. Note that this property does not configure the amount of RAM available to the container but is instead an upper limit. If the container's RAM exceeds this value it will cause docker to destroy and restart the container.

#### rabbit-instances
- Configures the number of instances of the RabbitMQ container to start on launch. This value also specifies the minimum number of instances the Autoscaler will scale the application down to.

#### rabbit-erlang-cookie
- This property is an arbitrary alphanumeric string that RabbitMQ uses to determine whether different Rabbit nodes in a cluster can communicate with each other. If two or more nodes share an identical cookie then RabbitMQ enables the nodes to communicate. Changing this value for another deployment of the RabbitMQ application will mean the two instances will be unable to cluster correctly.

#### rabbit-host
- This property is used to specify the IP or hostname of the machine the RabbitMQ application is running on. The property is not used by Rabbit itself but by each worker as part of the RabbitMQ configuration. Misconfiguring this value is the most common cause of a worker failing on start up. **Note this value must be replaced.**

#### rabbit-user
- This property specifies the username that RabbitMQ will use to create it's internal database on start up. This is the username supplied to each worker as part of the RabbitMQ connection configuration as well as the username to use to log into RabbitMQ's UI.

#### rabbit-password
- This property specifies the password that RabbitMQ will use for to create it's internal database on start up. This is the password supplied to each worker as part of the RabbitMQ connection configuration as well as the password to use to log into RabbitMQ's UI.

#### rabbit-port
- This property is used to specify the Port number of the RabbitMQ application is listening on. The property is not used by Rabbit itself but by each worker as part of the RabbitMQ configuration. The default value Rabbit listens to is 5672.

#### rabbit-maxattempts
- This property is used to specify the maximum number of connection attempts a worker will made before throwing a failure to connect exception and shutting down. This property is passed to all workers.

#### rabbit-backoffInterval
- This property is used by all workers to specify the time in seconds between each failed attempt to connect to the RabbitMQ server. This value will exponentially increase after each failure up to the value specified by `rabbit-maxBackoffInterval`

#### rabbit-maxBackoffInterval
- This property is used by all workers to specify the maximum time in seconds the back off interval can grow to after each consecutive failure.

#### rabbit-deadLetterExchange
- This property is meaningless, it is an arbitrary string that specifies a Dead Letter Exchange to the workers that the CAF framework never uses.

### example Properties
#### example-cpus
- Configures the amount of CPU of each example Worker container. This does not have to be a whole number.

#### example-mem
- Configures the amount of RAM of each example Worker container. Note that this property does not configure the amount of RAM available to the container but is instead an upper limit. If the container's RAM exceeds this value it will cause docker to destroy and restart the container.

#### example-java-mem-min
- Configures the minimum memory size available to Java. This value is used by the JVM to reserve an amount of system RAM on start up.

#### example-java-mem-max
- Configures the maximum memory size available to Java. This value is used by the JVM to specify the upper limit of system RAM the Java can consume. This limits the issue of Workers attempting to consume more memory that the container allows causing the application to fail.

#### example-8080-serviceport
- This property specifies the external port number on the host machine that will be forwarded to the Workers internal 8080 port. This port is used to call the workers health check.

#### example-8081-serviceport
- This property specifies the external port number on the host machine that will be forwarded to the Workers internal 8081 port. This port is used to retrieve metrics from the worker.

#### example-autoscale.metric
- This property maps to a label in the example workers application JSON that specifies to the autoscaler which metrics to use for scaling. This should ways be `rabbitmq`.

#### example-autoscale.scalingprofile
- The name of a scaling profile that has already been configured in the RabbitWorkloadAnalyserConfiguration resource within the autoscaler.

#### example-autoscale.maxinstances
- This number represents the maximum instances of the example worker the autoscaler have running.

#### example-autoscale.mininstances
- This number represents the minimum instances of the example worker the autoscaler have running. Cannot be set below zero.

#### example-healthcheck-graceperiodseconds
- This property specifies the time in seconds Marathon must wait before calling the example worker's health check. This allows the application time to finish starting up as a premature health check will return a failure.

#### example-healthcheck-intervalseconds
- This property specifies the time in seconds between each health check call.

#### example-healthcheck-maxconsecutivefailures
- This property specifies the maximum number of times the example worker can fail its health check before Marathon considers the application failed and restarts it.

#### healthcheck-timeoutseconds
- This property specifies the time in seconds Marathon will wait for the health check to return a response before failing the check.

#### example-resultSizeThreshold
- This property specifies the result size limit (in bytes) of the example worker at which the result will
be written to the DataStore rather than held in a byte array.

#### example-threads
- This property configures the number of threads the example Worker runs with.

#### example-version
- This property specifies the version number of the example Worker to pull down from Artifactory.
