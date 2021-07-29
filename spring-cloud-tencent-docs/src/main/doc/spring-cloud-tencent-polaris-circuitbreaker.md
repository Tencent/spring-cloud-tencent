# Polaris CircuitBreaker

## Module Intro

```spring-cloud-starter-tencent-polaris-circuitbreaker```is applied to Spring
Cloud project joint with [Polaris](https://github.com/polarismesh)'s CircuitBreaker module you can get cloud service engine's rate limit ability by introducing dependency. Recommended using with ```spring-cloud-starter-tencent-polaris-discovery```.

## Key Features

### Failed Node CircuitBreaker

Failed instance circuitbreak can achieve caller service's immediate auto-block high failure rate command instance, and set timed task to conduct live probing. When the recover status is achieved, one can start half recovery. After half recovery, release a few request to test probing. Identify the recovery status from the probing result.

### CircuitBreaker Strategy

- Failed ratio circuitbreak: when command instance at a service window (default one minute) request rate has reached or passed the minimum request threshold (default 10), and failure rate reached or passed failure ratio threshold (default 50%), instance will enter insolation state. Failure rate threshold range is [0.0, 1.0] , represent 0% - 100%.
- continuous failure circuitbreak: when command instance at a service window (default), continuous failure request reached or exceeded failure threshold (default 10), instance will inter insolation state.
- circuitbreak insolation time: default 30 seconds, support configuration

For configuration, please refer to [Polaris CircuitBreaker](https://github.com/polarismesh)

## User Guide

This chapter will explain how to use Polaris in Spring Cloud project in the simplest way.
CircuitBreaker's feature. Before starting MicroService, one needs to activate Polaris, activation details please refer to [Polaris](https://github.com/polarismesh).

1. you can add ```spring-cloud-starter-tencent-polaris-circuitbreaker``` 's dependencies in your project to use CircuitBreaker features. For example, in Maven's project, add listed configurations in pom:

```XML
<dependency>
    <groupId>com.tencent.cloud</groupId>
    <artifactId>spring-cloud-starter-tencent-polaris-circuitbreaker</artifactId>
</dependency>
```

2. For further instructions, please refer to  [Polaris CircuitBreaker Example](../../../../spring-cloud-tencent-examples/polaris-circuitbreaker-example/README.md).

## Configuration List

| Configuration Key                           | default | Must Fill | Configuration Instruction    |
| ------------------------------------------- | ------- | --------- | ---------------------------- |
| spring.cloud.polaris.circuitbreaker.enabled | true    | false     | Whether turn on CircuitBreak |

