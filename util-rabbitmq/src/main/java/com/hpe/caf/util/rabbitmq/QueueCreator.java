package com.hpe.caf.util.rabbitmq;


import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**
 * Builder pattern class to create queues with parameters and properties.
 * @since 2.0
 */
public class QueueCreator
{
    public static final String RABBIT_PROP_KEY_DEAD_EXCHANGE = "x-dead-letter-exchange";
    public static final String RABBIT_PROP_KEY_TTL = "x-message-ttl";
    public static final String RABBIT_PROP_KEY_DEAD_ROUTING_KEY = "x-dead-letter-routing-key";
    private Durability durability;
    private EmptyAction emptyAction;
    private Exclusivity exclusivity;
    private String queueName;
    private Map<String, Object> propsMap = new HashMap<>();


    public QueueCreator() { }


    public QueueCreator withDurability(Durability dur)
    {
        this.durability = dur;
        return this;
    }


    public QueueCreator withEmptyAction(EmptyAction action)
    {
        this.emptyAction = action;
        return this;
    }


    public QueueCreator withExclusivity(Exclusivity exclusivity)
    {
        this.exclusivity = exclusivity;
        return this;
    }


    public QueueCreator withQueueName(String name)
    {
        this.queueName = name;
        return this;
    }


    public QueueCreator withDeadLetterExchange(String exchangeName)
    {
        propsMap.put(RABBIT_PROP_KEY_DEAD_EXCHANGE, exchangeName);
        return this;
    }


    public QueueCreator withQueueTtl(long ttl)
    {
        propsMap.put(RABBIT_PROP_KEY_TTL, ttl);
        return this;
    }


    public QueueCreator withDeadLetterRoutingKey(String routingKey)
    {
        propsMap.put(RABBIT_PROP_KEY_DEAD_ROUTING_KEY, routingKey);
        return this;
    }


    public void create(Channel channel)
        throws IOException
    {
        validate();
        RabbitUtil.declareQueue(channel, queueName, durability, exclusivity, emptyAction, propsMap);
    }


    public void createWorkerQueue(Channel channel)
        throws IOException
    {
        withDurability(Durability.DURABLE).withExclusivity(Exclusivity.NON_EXCLUSIVE).withEmptyAction(EmptyAction.LEAVE_EMPTY);
        create(channel);
    }


    private void validate()
    {
        Objects.requireNonNull(queueName);
        Objects.requireNonNull(durability);
        Objects.requireNonNull(exclusivity);
        Objects.requireNonNull(emptyAction);
    }
}