---
layout: default
title: Worker Framework Overview

banner:
    icon: 'assets/img/hard-hat.png'
    title: Worker Framework
    subtitle: Analyze a Larger Range of Formats
    links:
        - title: GitHub
          url: https://github.com/WorkerFramework/worker-framework
          
---

# Overview

The Worker Framework provides a foundation for building cross-platform, cloud-ready, distributed data-processing microservices (workers). The framework is designed for massive scalability, redundancy, elasticity and resilience, which are achieved through architectural choices and technologies like Docker, Apache Mesos, Marathon and queue-based messaging with RabbitMQ. You can extend this framework through integration with many external and internal components, such as storage services.

The Worker Framework is a good fit for any data-processing scenario, including both on-premise and cloud-based solutions delivered as SaaS. For example, the Worker Framework could benefit solutions for content processing, mathematical and statistical analysis, and image and audio processing.

The Worker Framework is written entirely in Java and features the necessary infrastructure for execution of workers, messaging with fault-tolerant queues, monitoring, scaling, error handling and external data exchange. It also includes a set of base classes and interfaces allowing rapid development and integration.


## Introduction

Data processing on a massive scale brings many challenges. Load on a big data system can change instantly which means components need to automatically scale on demand. Any software or infrastructure (including hardware) faults have to be resolved with no human interaction. Since you have no time for a blocking activity, asynchronous processing is a must, which introduces the concept of a worker.

### The Worker
A worker is a service (to be more specific - a [microservice](http://martinfowler.com/articles/microservices.html)) designed to perform a specific type of task. This task contains input data and the worker is expected to process it and return a result or otherwise execute an action associated with the task. As a microservice, each worker is designed to do only one kind of work, and do it well. This specialization makes these components small, which brings many benefits, both in development and execution.

Workers rely on messaging queues and the task is simply an input message, which results in an output message. The service listens to an assigned queue. When a message appears, it picks it up, processes it, and generates a result message on a configured output queue.

Workers are stateless. In big data applications, stateful components are problematic because they are much harder to scale, and resiliency is a challenge. Workers do not keep any internal state and can be scaled up or down to meet demand. In essence, a worker need not be aware of any other worker or its surrounding system. Additional "clones" of a worker can be created or destroyed at will to scale without affecting other workers.


### The Framework

The Worker Framework makes the development of workers as straightforward and easy as possible. It also enforces the same, consistent pattern during implementation. 
It's a lightweight, pure Java library created with best practices in mind.

Internally, the framework, solves a few challenges, including fault tolerance, high availability and configuration.

The Worker Framework is highly extensible. Most of its components are pluggable and can easily be extended or replaced with a different implementation. This includes configuration sources, external data storage or even the messaging components. It was designed for use in orchestration platforms, like Marathon and Mesos, and has support for health monitoring and auto-scaling. 