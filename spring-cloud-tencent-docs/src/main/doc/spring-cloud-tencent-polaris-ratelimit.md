# Spring Cloud Starter Tencent Polaris RateLimit

## Module Introduction

```spring-cloud-starter-tencent-polaris-ratelimit``` is used in Spring
Cloud project joint with [Polaris](https://github.com/polarismesh)'s rate limit component.
You can access Microservice's rate limit through dependencies. Recommended using with ```spring-cloud-starter-tencent-polaris-discovery```

## Features Introduction

### Service Rate Limit

Provide rate limit feature to all HTTP server.

Default introduce spring-cloud-starter-tencent-polaris-ratelimit dependencies can apply rate limit check to all HTTP server.

### API Rate Limit

Provide rate limit feature to all HTTP server depending on the path level

Default introduce spring-cloud-starter-tencent-polaris-ratelimit dependencies can apply rate limit check to all HTTP server path.

## User Guide

This chapter will explain how to use Polaris RateLimit in Spring Cloud project with the easiest way.  Before starting MicroService, one needs to launch Polaris. Please refer to [Polaris Getting Started](https://github.com/PolarisMesh/polaris#getting-started).

1. You can add ```spring-cloud-starter-tencent-polaris-ratelimit```‘s dependencies in your project to use the rate limit feature.  For example, in Maven's project, add listed:

```XML
<dependency>
    <groupId>com.tencent.cloud</groupId>
    <artifactId>spring-cloud-starter-tencent-polaris-ratelimit</artifactId>
</dependency>
```

2. Add rate limit configuration

Polaris provides three different configuration methods, control panel operation, HTTP API upload and local files configuration, further information please refer to [Polaris service rate limit operation guide](https://github.com/polarismesh)

For more details, please refer to [Polaris RateLimit Example](../../../../spring-cloud-tencent-examples/polaris-ratelimit-example/README.md)

## Configuration list

| Configuration Key                      | default | yes/no required | Configuration Instruction     |
| -------------------------------------- | ------- | --------------- | ----------------------------- |
| spring.cloud.polaris.ratelimit.enabled | true    | false           | whether to turn on rate limit |

