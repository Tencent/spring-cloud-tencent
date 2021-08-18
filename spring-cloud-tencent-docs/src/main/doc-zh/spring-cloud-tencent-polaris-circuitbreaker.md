# Polaris CircuitBreaker

## 模块简介

```spring-cloud-starter-tencent-polaris-circuitbreaker```是用于Spring
Cloud项目对接服务治理平台[Polaris](https://github.com/polarismesh)的故障熔断模块。
您可以通过引入依赖即可获得对微服务架构的服务限流能力。建议与```spring-cloud-starter-tencent-polaris-discovery```配合使用。

## 功能介绍

### 故障节点熔断

故障实例熔断能实现主调方迅速自动屏蔽错误率高或故障的服务实例，并启动定时任务对熔断实例进行探活。在达到恢复条件后对其进行半开恢复。在半开恢复后，释放少量请求去进行真实业务探测。并根据真实业务探测结果去判断是否完全恢复正常。

### 熔断策略
- 故障比例熔断：当服务实例在上一个时间窗（默认1分钟）内，通过的请求量达到或超过最小请求阈值（默认10个），且错误率达到或超过故障比率阈值（默认50%），实例会进入隔离状态。故障比率的阈值范围是 [0.0, 1.0]，代表 0% - 100%。
- 连续故障熔断：当实例在上一个时间窗（默认1分钟）内，连续失败的请求数达到或者超过连续故障阈值（默认10个），实例会进入隔离状态。
- 熔断隔离时间：默认隔离30s，支持可配置。

相关配置请参考[PolarisJava使用文档](https://github.com/PolarisMesh/polaris-java)

## 快速入门

本章节将介绍如何最简单地在Spring Cloud项目中使用Polaris
CircuitBreaker的功能。启动微服务之前，需要启动Polaris，具体启动方式参考[Polaris Getting Started](https://github.com/PolarisMesh/polaris#getting-started)。

1. 您可以在项目中加入```spring-cloud-starter-tencent-polaris-circuitbreaker```依赖即可使用故障熔断的特性。如Maven项目中，在pom中添加如下配置：

```XML
<dependency>
    <groupId>com.tencent.cloud</groupId>
    <artifactId>spring-cloud-starter-tencent-polaris-circuitbreaker</artifactId>
</dependency>
```

2. 更加详细的使用方法参考 [Polaris CircuitBreaker Example](../../../../spring-cloud-tencent-examples/polaris-circuitbreaker-example/README-zh.md)。

## 配置列表

| 配置项Key                                       | 默认值                     |是否必填| 配置项说明                   |
| ----------------------------------------------- | -----------------------| --------- | ---------------------------- |
| spring.cloud.polaris.circuitbreaker.enabled    | true                      |否| 是否开启故障熔断   |

