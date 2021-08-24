# Spring Cloud Tencent Polaris Gateway

## Module Introduction

```spring-cloud-tencent-polaris-gateway``` is used in Spring Cloud project joint with 
[Polaris](https://github.com/PolarisMesh/polaris)'s Polaris Gateway component.
You can use Polaris's extension microservice gateway features by importing dependency.

## Features Instruction

### Metadata Delivery

Support metadata delivery between gateways and microservices.

### Gateway RateLimit

Support rate limit of gateway routing to the corresponding micro-service.

## User Guide

This chapter will explain how to use Spring Cloud Tencent Polaris Gateway's features in the Spring Cloud project. 
Before starting MicroService, one needs to launch Polaris. 
Please refer to [Polaris Getting Started](https://github.com/PolarisMesh/polaris#getting-started).

1. you can add ```spring-cloud-tencent-polaris-gateway``` in your project to use Polaris's microservice gateway extension features
(meaning you still need to add microservice gateway modules yourself, such as zuul, spring-cloud-gateway). 
For example, in Maven's project, add listed configurations in pom:

```XML
<dependency>
    <groupId>com.tencent.cloud</groupId>
    <artifactId>spring-cloud-tencent-polaris-gateway</artifactId>
</dependency>
```

2. For further instructions, please refer to  [Polaris Gateway Example](../../../../spring-cloud-tencent-examples/polaris-gateway-example/README.md).

## Feature Usage

### Metadata Delivery

Because making use of ```spring-cloud-tencent-metadata```, please refer to [Spring Cloud Tencent Metadata](spring-cloud-tencent-metadata.md).

### Gateway RateLimit

Because making use of ```spring-cloud-starter-tencent-polaris-ratelimit```, please refer to [Spring Cloud Tencent Metadata](spring-cloud-tencent-polaris-ratelimit.md).

