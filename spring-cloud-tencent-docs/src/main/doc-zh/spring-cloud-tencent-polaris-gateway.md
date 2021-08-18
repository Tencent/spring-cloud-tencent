# Spring Cloud Tencent Polaris Gateway

## 模块简介

```spring-cloud-tencent-polaris-gateway```是用于Spring Cloud项目对接服务治理平台[Polaris](https://github.com/PolarisMesh/polaris)的微服务网关模块。
您可以通过引入依赖即可使用Polaris拓展的微服务网关功能。

## 功能介绍

### 元数据传递

支持网关和微服务之间的元数据传递。

## 快速入门

本章节将介绍如何最简单地在Spring Cloud项目中使用Spring Cloud Tencent Polaris Gateway的功能。
启动微服务之前，需要启动Polaris，具体启动方式参考[Polaris Getting Started](https://github.com/PolarisMesh/polaris#getting-started)。

1. 您可以在项目中加入```spring-cloud-tencent-polaris-gateway```依赖即可使用Polaris的微服务网关拓展功能（意味着您还是需要自行添加微服务网关模块，如zuul、spring-cloud-gateway）。如Maven项目中，在pom中添加如下配置：

```XML

<dependency>
    <groupId>com.tencent.cloud</groupId>
    <artifactId>spring-cloud-tencent-polaris-gateway</artifactId>
</dependency>
```

2. 更加详细的使用方法参考 [Polaris Gateway Example](../../../../spring-cloud-tencent-examples/polaris-gateway-example/README-zh.md)。

## 功能使用

### 元数据传递

其依赖的是```spring-cloud-tencent-metadata```模块的功能，因此使用上参考[Spring Cloud Tencent Metadata](spring-cloud-tencent-metadata.md)即可。
