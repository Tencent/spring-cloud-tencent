# Polaris RateLimit

## 模块简介

```spring-cloud-starter-tencent-polaris-ratelimit```是用于Spring
Cloud项目对接服务治理平台[Polaris](https://github.com/polarismesh)的服务限流模块。
您可以通过引入依赖即可获得对微服务架构的服务限流能力。建议与```spring-cloud-starter-tencent-polaris-discovery```配合使用。

## 功能介绍

### 服务级限流

支持为所有的HTTP服务提供限流功能。 

默认引入spring-cloud-starter-tencent-polaris-ratelimit依赖即可对所有的HTTP服务执行限流检查。

### 接口级限流

支持为所有的HTTP调用根据path级别的提供限流功能。

默认引入spring-cloud-starter-tencent-polaris-ratelimit依赖即可对所有的HTTP path调用执行限流检查。

## 快速入门

本章节将介绍如何最简单地在Spring Cloud项目中使用Polaris
RateLimit的功能。启动微服务之前，需要启动Polaris，具体启动方式参考[Polaris](https://github.com/polarismesh)。

1. 您可以在项目中加入```spring-cloud-starter-tencent-polaris-ratelimit```依赖即可使用服务限流的特性。如Maven项目中，在pom中添加如下配置：

```XML
<dependency>
    <groupId>com.tencent.cloud</groupId>
    <artifactId>spring-cloud-starter-tencent-polaris-ratelimit</artifactId>
</dependency>
```

2. 添加限流规则配置

北极星提供了三种添加限流配置的方式，包括控制台操作、HTTP接口上传和本地文件配置，具体请参考[北极星服务限流使用文档](https://github.com/polarismesh)

更加详细的使用方法参考 [Polaris RateLimit Example](../../../../spring-cloud-tencent-examples/polaris-ratelimit-example/README-zh.md)。

## 配置列表

| 配置项Key                                       | 默认值                     |是否必填| 配置项说明                   |
| ----------------------------------------------- | -----------------------| --------- | ---------------------------- |
| spring.cloud.polaris.ratelimit.enabled          | true                       |否| 是否开启服务限流   |

