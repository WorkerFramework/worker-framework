# util-rabbitmq

---

 This subproject contains useful utility components for interacting with a
 RabbitMQ queue.


## RabbitUtil

 This class contains utility methods for creating a RabbitMQ connection and
 declaring queues. All queues must be declared with RabbitMQ before trying to
 utilise them (from either a producer or consumer side) and the declaration
 parameters must be the same, hence the need for some agreed upon manner to
 declare these queues.

### Creating a connection

 To create a connection, firstly create a `RabbitConfiguration` object
 which can be created programmatically or otherwise utilise something like a
 `ConfigurationSource` from the `caf-api` to deserialise configuration from a
 remote source. See the section on configuration for more details on this
 object.

 To create a connection, pass this object to a method inside
 `RabbitUtil` and a wrapped AMQP client connection will be returned:

 ```
 Connection c = RabbitUtil.createRabbitConnection(config);
 ```

 This `Connection` will be handled by the Lyra client library, which will
 automatically deal with connection drops and failed communication attempts,
 up to the limits specified within the configuration. If a greater level of
 control over Lyra is needed, such as adding `ConnectionListener` classes,
 create a connection with a sequence such as this:

 ```
 ConnectionOptions opt = RabbitUtil.createLyraConnectionOptions(config);
 Config cfg = RabbitUtil.createLyraConfig(config);
 cfg.withConnectionListeners(new CustomConnectionListener());
 Connection c = RabbitUtil.createRabbitConnection(opt, cfg);
 ```

### Creating a channel

 After a connection is created, a `Channel` is needed. All data and requests
 in RabbitMQ flow over a `Channel` and it is not limited to one per connection.
 Typically there is one channel for input and one for output, created by:

 ```
 Channel channel = c.createChannel();
 ```

### Declaring queues

 Remember to make sure a queue has been declared before utilising it.
 It does not matter if a queue is declared repeatedly, as long as the
 declaration parameters are the same. Since all data flows over a `Channel`,
 a queue and a channel msut be tied together. There is a supplied method for
 declaring queues specifically for usage with workers:

 ```
 RabbitUtil.declareWorkerQueue(channel, queueName);
 ```

 However, it is possible to create queues with different properties. Three
 different aspects or settings make up the properties of a queue, which are:

 - durability: whether these queues are disk backed (persistent) or just
  temporary
 - exclusivity: whether this queue is only to be used by this channel or not
 - empty action: whether to destroy this queue when it becomes empty or not

 A worker queue is durable, non-exclusive, and permanent (not destroyed when
 empty). The main one of interest here is durability. If operating
 a temporary it does not have to be durable. Non-durable queues are much
 much faster, so deliver extremely high performance message delivery.
 Use the `Durability`, `Exclusivity` and `EmptyAction` enums to pass the
 desired settings to `RabbitUtil` similar to this:

 ```
 RabbitUtil.declareQueue(channel, queue, durability, exclusivity, emptyAction);
 ```


## Creating producers and consumers

 This package also contains some base classes which are useful for creating
 your own producers and consumers. The basic concept of these is that they run
 in their own thread and listen upon their own internal event queue to interact
 with RabbitMQ. This completely decouples message delivery from processing and
 ensures that the same thread can handle events of the appropriate type.

 Both consumers and producers have a parent of `EventPoller`. Custom producers
 should directly use or extend this. However, all consumers should extend
 `RabbitConsumer` as it has additional code for dealing with the RabbitMQ
  client library.

 Two default classes are available to use: the `DefaultRabbitPublisher` and the
 `DefaultRabbitConsumer`. These should be sufficient for basic use cases.

 The method of operation is as follows. An `EventPoller` is always created with
 a type of `Event<T>`, where an `Event` itself is a functional interface that
 calls back to a target when triggered. This target is generally an interface
 that implements all the methods the `EventPoller` wishes to trigger. In the
 case of the `DefaultRabbitPublisher`, this is the `QueuePublisher` interface,
 and the `DefaultRabbitConsumer` uses the `QueueConsumer` interface. Hence,
 the `EventPoller` will need an implementation of this interface to which the
 events it receives will call to. In effect, code using this library should
 create new classes that implement `QueueConsumer` and/or `QueuePublisher` as
 appropriate. Typically, they will take a RabbitMQ `Channel` to perform the
 necessary operations upon in their constructor. This implementation is then
 handed to the `DefaultRabbitPublisher` or `DefaultRabbitConsumer` in its
 constructor. The publisher/consumer will then call through to the created
 implementation as it receives events of either `Event<QueueConsumer>` or
 `Event<QueuePublisher>` as appropriate.

 For more complicated scenarios, it may be desirable to have a different
 event sequence than `QueuePublisher` or `QueueConsumer`. In this case,
 developers should create their own interface and then for a publisher
 instantiate an `EventPoller<T>` where `T` is the new interface, or a
 `RabbitConsumer<T>` for a consumer. The various `Event<T>` classes can then be
 made and added to the internal queue that either the consumer or producer use.

 For the `worker-queue-rabbit` module, a more complex scenario than basic
 publishing is required. Here, a message can be published to any queue rather
 than a fixed one, and an original (different) message acknowledged. See the
 `worker-queue-rabbit` code repository if you need an example like this.

 For using the supplied `QueuePublisher` and `QueueConsumer` interfaces, the
 following `Event<T>` classes are available:

  - ConsumerAckEvent
  - ConsumerDeliverEvent
  - ConsumerDropEvent
  - ConsumerRejectEvent
  - PublisherPublishEvent

 The general workflow will look like this:

 ```
 Connection conn = RabbitUtil.createRabbitConnection(getRabbitConfig());
 BlockingQueue<Event<QueueConsumer>> q = new LinkedBlockingQueue<>();
 Channel ch = conn.getChannel();
 // create this class, MyQueueConsumer implements QueueConsumer
 QueueConsumer impl = new MyQueueConsumer(ch);
 String queue = "myQueue";
 RabbitUtil.declareWorkerQueue(ch, queue);
 DefaultRabbitConsumer consumer = new DefaultRabbitConsumer(q, impl);
 String consumerTag = ch.basicConsume(queue, consumer);
 new Thread(consumer).start();
 // put events onto the q BlockingQueue here
 // shutdown process
 ch.basicCancel(consumerTag);
 consumer.shutdown();
 ch.close();
 conn.close();
 ```


## Maintainers

 The following people are contacts for developing and maintaining this module:

 - Richard Hickman (Cambridge, UK, richard.hickman@hp.com)
