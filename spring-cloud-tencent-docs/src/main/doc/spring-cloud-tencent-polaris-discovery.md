# Spring Cloud Starter Tencent Polaris Discovery

## Module Introduction

```spring-cloud-starter-tencent-polaris-discovery``` is used in Spring Cloud project joint with [Polaris](https://github.com/polarismesh)'s Polaris Discovery component. You can complete Microservice registration at Polaris through dependencies, get visibility and control to the entire Cloud Service Engine.

## Features Instruction

### Service Registration and Discovery

Spring Cloud API to achieve service registration and discovery.

### Service Route

Foundation built on Ribbon's API allows multiple application's dynamic service route, provided by Polaris's policy route feature. One can assign and control tasks through this feature. Through this feature, one can easily adapt different applications,  SET routing, greyscale release, disaster recovery degrade, and canary test etc.

Meanwhile, users can use independent discovery component's custom data feature to program routing, further improve its agility and performance.

### CLoud Load Balance

CLB supports qualified packet forwarding in the service instance. Through set balancing, send a selected instance to the caller service, to support caller's service request. CLB rule includes random weight policy, weight response time and coordinated Hash.

## User Guide

This chapter will explain how to use Polaris Discovery's features in the Spring Cloud project. Before starting MicroService, one needs to launch Polaris. Please refer to [Polaris Getting Started](https://github.com/PolarisMesh/polaris#getting-started).

1. you can add ```spring-cloud-starter-tencent-polaris-discovery```'s  's dependencies in your project to use Polaris's service registration and discovery feature. For example, in Maven's project, add listed configurations in pom:

```XML
<dependency>
    <groupId>com.tencent.cloud</groupId>
    <artifactId>spring-cloud-starter-tencent-polaris-discovery</artifactId>
</dependency>
```

2. Add listed configurations in the document, one can complete service registration and discovery (after Spring Cloud Edgware, ```@EnableDiscoveryClient``` is no longer needed to run service registration and discovery):

```yaml
spring:
  application:
    name: ${application.name}
  cloud:
    polaris:
      server-addr: ${ip}:${port}
```

For further instructions, please refer to [Polaris Discovery Example](../../../../spring-cloud-tencent-examples/polaris-discovery-example/README.md).

## Extended Application

### Service Route

- you can configure routing policy at Polaris control panel, and use the features.
- you can add custom metadata at configure documentations (application.yml), Then configure routing policy at Polaris control panel, one can also use the features. Example listed below, this will be read as Metadata Map.

```
spring:
  cloud:
    tencent:
      content:
        a: 1
        b: 2
```

### Cloud Load Balancing

Taking examples like random weight policy, you can add weight at Polaris control panel or configure documentations (application.yml) to access CLB features.

## Configuration LIst

| ConfigurationKey                                | default                    | Yes/No required | Configuration Instruction                            |
| ----------------------------------------------- | -------------------------- | --------------- | ---------------------------------------------------- |
| spring.cloud.polaris.server-addr                | false                      | yes             | Polaris backend address                              |
| spring.cloud.polaris.discovery.service          | ${spring.application.name} | null            | service name                                         |
| spring.cloud.polaris.discovery.enabled          | true                       | false           | whether to active service registration and discovery |
| spring.cloud.polaris.discovery.instance-enabled | true                       | false           | can current Microservice be visited                  |
| spring.cloud.polaris.discovery.token            | false                      | false           | Authentication Token                                 |
| spring.cloud.polaris.discovery.version          | null                       | false           | Microservice Version                                 |
| spring.cloud.polaris.protocol                   | null                       | false           | Microservice agreement type                          |
| spring.cloud.polaris.weight                     | 100                        | false           | Microservice weight                                  |
| spring.cloud.loadbalancer.polaris.enabled       | true                       | false           | whether to open CLB                                  |
| spring.cloud.loadbalancer.polaris.strategy      | weighted_random            | false           | CLB policy                                           |
| spring.cloud.tencent.metadata.content           | null                       | false           | custom metadata Map                                |
| spring.cloud.tencent.metadata.transitive        | null                       | false           | need custom metadata key list                        |

