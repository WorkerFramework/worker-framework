---
layout: default
title: Features

banner:
    icon: 'assets/img/hard-hat.png'
    title: Worker Framework
    subtitle: Analyze a Larger Range of Formats
    links:
        - title: GitHub
          url: https://github.com/WorkerFramework/worker-framework
          
---

# Key Features

The Worker Framework includes the following key features.

## High availability and fault tolerant messaging
By default, the Worker Framework uses [RabbitMQ](https://www.rabbitmq.com/), a reliable, robust messaging platform, which allows asynchronous processing and protects against data loss. It also utilizes [Lyra](https://github.com/jhalterman/lyra), a high availability client for RabbitMQ, which provides automated recovery from failures.

## External data exchange
Workers usually operate on data that's provided from an external, distributed storage system. Also, the size of data can get quite large, including results produced by a worker. To optimize processing, the Worker Framework has a built-in feature allowing data exchange with external storage. It is a pluggable solution and comes with Amazon S3 Storage plugin.

## Configuration Management
Every worker requires some configuration, which can include queue names it should use or external storage information. The Worker Framework provides developers with an easy way of reading any type of configuration. It supports configuration management by allowing the configuration to be retrieved from various sources like files or REST services. Configuration sources are pluggable so you can extend it to suit your needs.

## Health Monitoring
In a distributed system, failures are inevitable. Components have to recover on their own, with no human interaction. Orchestration platforms like Marathon take care of recovery, but they need to know when a service is failing. The Worker Framework supports that by providing a health-check endpoint. Its design enforces this practice by requiring you to return health status upon request. The framework takes care of the rest.

## Serialization and De-serialization of Message Payload
Message brokers usually operate on a stream of data. This data contains information required by a particular worker. Since workers operate on typed objects, the message data needs to be serialized and de-serialized. The Worker Framework takes care of that internally such that you don't have to deal with un-typed data streams.

## Bulk Message Handling
While workers usually operate on a single message at a time, the Worker Framework provides facilities to implement services designed to handle multiple messages in bulk. The Worker Framework gives a worker full control over partitioning of batches as well as acknowledgement of the completion. This feature is flexible enough for use in a variety of scenarios where bulk processing brings performance benefits, including database processing or search engine ingestion.
