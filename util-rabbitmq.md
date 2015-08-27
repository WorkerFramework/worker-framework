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

 To create a connection, firstly you will need a `RabbitConfiguration` object
 which can be created programmatically or otherwise utilise something like a
 `ConfigurationSource` from the `caf-api` to deserialise configuration from a
 remote source. See the section on configuration for more details on this
 object.

 To create a connection, you will then pass this object to a method inside
 `RabbitUtil` and a wrapped AMQP client connection will be returned:

 ```
 Connection c = RabbitUtil.createRabbitConnection(config);
 ```

 This `Connection` will be handled by the Lyra client library, which will
 automatically deal with connection drops and failed communication attempts,
 up to the limits specified within the configuration.

### Creating a channel

 Now you have a connection, you will need a `Channel`. All data and requests
 in RabbitMQ flow over a `Channel` and it is not limited to one per connection.
 Typically you may use one channel for inbound and one for outbound but it is
 not limited to this:

 ```
 Channel channel = c.createChannel();
 ```

### Declaring queues

 Now remember that you must make sure you have declared a queue once before you
 try and use it. It does not matter if you call declare and it has already been
 declared, as long as the declaration parameters are the same. Since all data
 flows over a `Channel`, you must declare the queue using your `Channel`. If
 you are using the queue for worker purposes (ie. it is a queue that you are
 publishing to for workers to consume, or the code is an actual worker that is
 expecting tasks), then you can use this method:

 ```
 RabbitUtil.declareWorkerQueue(channel, queueName);
 ```

 However, if you are using this library for your own purposes, you may wish to
 create queues with different properties. Three different aspects or settings
 make up the properties of a queue, which are:

 - durability: whether these queues are disk backed (persistent) or just
  temporary
 - exclusivity: whether this queue is only to be used by this channel or not
 - empty action: whether to destroy this queue when it becomes empty or not

 A worker queue is durable, non-exclusive, and permanent (not destroyed when
 empty). The main one of interest here is durability. If you are operating
 a temporary queue or do not need the contents of the queue to be reliably
 persisted then it does not have to be durable. Non-durable queues are much
 much faster, so if you need extremely high performance message delivery you
 may want to consider this. You can use the `Durability`, `Exclusivity` and
 `EmptyAction` enums to pass the settings to `RabbitUtil` similar to this:

 ```
 RabbitUtil.declareQueue(channel, queue, durability, exclusivity, emptyAction);
 ```


## Creating producers and consumers

 This package also contains some base classes which are useful for creating
 your own producers and consumers. The basic concept of these is that they run
 in their own thread and listen upon their own internal event queue to interact
 with RabbitMQ. The complexity of this comes from the fact the same thread that
 received a message should be the same one to acknowledge it. Most of this is
 hidden away, however.

 The base class for producers is a `RabbitPublisher`, which is of the type
 `T extends QueueEvent<PublishEventType>`, where `PublishEventType` is an enum
 of event types a `RabbitPublisher` can handle, and a `QueueEvent` is a wrapper
 that will contain data relevant to the event for processing. A typical use
 case class is provided here in the form of the `PublishQueueEvent`, which
 contains a byte array of data which represents the message to publish. For a
 publisher, the enum only contains a single event type.

 Creation of a very basic new publisher would work as follows:

 ```
 public class MyPublisher extends RabbitPublisher<PublishQueueEvent>
 {
    public MyPublisher(BlockingQueue<PublishQueueEvent> ev, final Channel ch)
    {
        super(ev, ch);
    }


    @Override
    protected void handlePublish(PublishQueueEvent event)
    {
        getChannel().basicPublish("", event.getQueue(),
                                  MessageProperties.PERSISTENT_TEXT_PLAIN,
                                  event.getEventData());
    }
 }
 ```

 A consumer is slightly more complex simply because of the amount of methods
 it has to implement, but is fundamentally the same. The signature of a
 `RabbitConsumer` is of the type `T extends QueueEvent<ConsumerEventType>`
 where the `ConsumerEventType` here includes the following events:

 - DELIVER: a new message has been delivered
 - ACK: acknowledge a previously delivered message
 - REJECT: push a received message back onto the queue to handle again
 - DROP: a rejection without requeue

 As such, an implementation of a `RabbitConsumer` has to have the following
 methods:

 - processDelivery(Delivery, T)
 - processAck(T)
 - processRejec(T)
 - processDrop(T)
 - getDeliverEvent(long)

 The only two more complicated methods here are `processDelivery` and
 `getDeliverEvent`, as the rest are fairly self-explanatoy. The former of these
 takes a RabbitMQ `Delivery` object, which contains the envelope with message
 properties (including whether it is a re-delivery) and also the message data
 itself in byte form. The other one `getDeliverEvent` should simply return a
 new instance of your given `QueueEvent<ConsumerEventType>` class, with the
 event type set to `DELIVER` and the appropriate tag set from the passed in
 long parameter. This is unfortunately, just boilerplate that is necessary to
 allow implementors to use any implementation of `QueueEvent` that they wish.
 If you are using the supplied `ConsumerQueueEvent` class, your method may
 look like this:

 ```
 @Override
 protected ConsumerQueueEvent getDeliverEvent(final long tag)
 {
    return new ConsumerQueueEvent(ConsumerEventType.DELIVER, tag);
 }
 ```

 Once you have written your implementation(s), remember that these are runnable
 threads that need to be started! The general workflow will look like this:

 ```
 Connection conn = RabbitUtil.createRabbitConnection(getRabbitConfig());
 BlockingQueue<PublishQueueEvent> q = new LinkedBlockingQueue<>();
 Channel ch = conn.getChannel();
 String queue = "myQueue";
 RabbitUtil.declareWorkerQueue(ch, queue);
 MyPublisher pub = new MyPublisher(q, ch);
 new Thread(pub).start();
 // put events onto the q BlockingQueue here...
 pub.shutdown();
 ```


## Maintainers

 The following people are contacts for developing and maintaining this module:

 - Richard Hickman (Cambridge, UK, richard.hickman@hp.com)
