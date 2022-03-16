# Polaris Discovery

## 模块简介

```spring-cloud-starter-tencent-polaris-discovery```是用于Spring
Cloud项目对接服务治理平台[Polaris](https://github.com/PolarisMesh/polaris)的服务发现模块。您可以通过引入依赖即可完成微服务注册到服务治理平台Polaris，获得对整个微服务架构的服务治理能力。

## 功能介绍

### 服务注册与发现

基于Spring Cloud的标准接口实现服务注册与发现。

### 服务路由

基于Ribbon的标准接口实现的支持多种场景的动态服务路由，是北极星提供规则路由的能力，通过规则来动态控制消息的分配转发。通过该功能，您者可以轻松实现多环境路由、分SET路由、灰度发布、集群容灾降级、金丝雀测试等功能。

同时，用户也可以利用独立于服务发现模块的自定义元数据功能来形成路由规则进行规则路由，进一步提升服务路由的灵活性。

### 负载均衡

负载均衡支持从满足本次转发要求的服务实例集中， 通过一定的均衡策略，选取一个实例返回给主调方，供主调方进行服务请求发送。负载均衡规则包括权重随机策略、权重响应时间策略和一致性哈希算法。

## 快速入门

本章节将介绍如何最简单地在Spring Cloud项目中使用Polaris
Discovery的功能。启动微服务之前，需要启动Polaris，具体启动方式参考[Polaris Getting Started](https://github.com/PolarisMesh/polaris#getting-started)。

1. 您可以在项目中加入```spring-cloud-starter-tencent-polaris-discovery```依赖即可使用Polaris的服务注册与发现功能。如Maven项目中，在pom中添加如下配置：

```XML

<dependency>
    <groupId>com.tencent.cloud</groupId>
    <artifactId>spring-cloud-starter-tencent-polaris-discovery</artifactId>
</dependency>
```

2. 在配置文件中主要添加如下配置，即可完成服务注册与发现（在Spring Cloud Edgware之后，无需使用```@EnableDiscoveryClient```即可进行服务注册与发现）：

```yaml
spring:
  application:
    name: ${application.name}
  cloud:
    polaris:
      address: ${protocol}://${ip}:${port}
```

3. 如果您部署的是单机版Polaris，您需要在项目中添加如下Polaris参数（.../resources/polaris.yml）：

```yaml
global:
  system:
    discoverCluster:
      sameAsBuiltin: true
    healthCheckCluster:
      sameAsBuiltin: true
```

更加详细的使用方法参考 [Polaris Discovery Example](../../../../spring-cloud-tencent-examples/polaris-discovery-example/README-zh.md)。

## 拓展使用

### 服务路由

- 您可以在Polaris控制台页面上配置路由规则，即可使用服务路由的功能。
- 您也可以在配置文件（application.yml）中添加自定义元数据，然后再Polaris控制台页面上配置路由规则，也可使用服务路由的功能。样例配置如下所示，在应用运行时将读为Map的数据格式。

```
spring:
  cloud:
    tencent:
      content:
        a: 1
        b: 2
```

### 负载均衡

以权重随机策略为例，您可以在Polaris控制台页面上或者配置文件（application.yml）中添加权重值，即可使用负载均衡的功能。

## 配置列表

| 配置项Key                                          | 默认值                        | 是否必填 | 配置项说明                     |
|-------------------------------------------------|----------------------------|------|---------------------------|
| spring.cloud.polaris.server-addr                | 无                          | 是    | Polaris后端地址               |
| spring.cloud.polaris.discovery.service          | ${spring.application.name} | 否    | 服务名称                      |
| spring.cloud.polaris.discovery.enabled          | true                       | 否    | 是否开启服务注册与发现               |
| spring.cloud.polaris.discovery.namespace        | default                    | 否    | 服务所在的命名空间名称               |
| spring.cloud.polaris.discovery.instance-enabled | true                       | 否    | 当前微服务实例是否可以被访问            |
| spring.cloud.polaris.discovery.token            | 无                          | 否    | 鉴权Token                   |
| spring.cloud.polaris.discovery.version          | null                       | 否    | 微服务版本                     |
| spring.cloud.polaris.discovery.ip-address       | null                       | 否    | 注册的IP地址                   |
| spring.cloud.polaris.protocol                   | null                       | 否    | 微服务协议类型                   |
| spring.cloud.polaris.weight                     | 100                        | 否    | 微服务权重                     |
| spring.cloud.loadbalancer.polaris.enabled       | true                       | 否    | 是否开启负载均衡                  |
| spring.cloud.loadbalancer.polaris.strategy      | weighted_random            | 否    | 负载均衡策略                    |
| spring.cloud.tencent.metadata.content           | 无                          | 否    | 自定义元数据，为Map结构             |
| spring.cloud.tencent.metadata.transitive        | 无                          | 否    | 需要传递的自定义元数据的key列表，为List结构 |

