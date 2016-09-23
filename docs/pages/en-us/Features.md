---
layout: default
title: Features
---

# Key Features

The Worker Framework includes the following key features.

## High availability and fault tolerant messaging

[RabbitMQ](https://www.rabbitmq.com/) is a reliable, robust messaging platform, which allows asynchronous processing and protects against data loss. [Lyra](https://github.com/jhalterman/lyra) is a high availability client for RabbitMQ, providing automated recovery from failures.

## External data exchange

Workers usually operate on data that's provided from an external, distributed storage system. Also, the size of data can get quite large, including results produced by a worker. To optimize processing, the Worker Framework has a built-in feature allowing data exchange with external storage. It is a pluggable solution and comes with Amazon S3 Storage plugin.

## Configuration Management
Every worker requires some configuration, which can include queue names it should use or external storage information. The Worker Framework provides developers with an easy way of reading any type of configuration. It supports configuration management by allowing the configuration to be retrieved from various sources like files or REST services. Configuration sources are pluggable so you can extend it to suit your needs.

## Health Monitoring
In a distributed system, failures are inevitable. Components have to recover on their own, with no human interaction. Orchestration platforms like Marathon take care of recovery, but they need to know when a service is failing. The Worker Framework supports that by providing a health-check endpoint. Its design enforces this practice by requiring you to return health status upon request. The framework takes care of the rest.

