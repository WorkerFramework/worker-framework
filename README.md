# Worker Framework

The Worker Framework provides a foundation for cross-platform, cloud-ready, distributed data-processing microservices (workers). The framework is designed for massive scalability, redundancy, elasticity and resilience, which are achieved through architectural choices and technologies like Docker, Apache Mesos, Marathon and queue-based messaging with RabbitMQ. You can extend this framework through integration with many external and internal components, such as storage services.

The Worker Framework is a good fit for any data-processing scenario, including both on-premise and cloud-based solutions delivered as SaaS. For example, the Worker Framework could benefit solutions for content processing, mathematical and statistical analysis, and image and audio processing.

The Worker Framework is written entirely in Java and features the necessary infrastructure for execution of workers, messaging with fault-tolerant queues, monitoring, scaling, error handling and external data exchange. It also includes a set of base classes and interfaces allowing rapid development and integration.

Manifest of the components which make up the CAF Worker Framework:
* standard-worker-container
* util-rabbitmq
* worker-api
* worker-archetype
* worker-caf
* worker-configs
* worker-core
* worker-example
* worker-framework
* worker-queue-rabbit
* worker-store-cs
* worker-store-fs
* worker-store-s3
* worker-testing-integration
* worker-testing-util
