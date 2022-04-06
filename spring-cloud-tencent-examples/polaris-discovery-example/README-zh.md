# Spring Cloud Polaris Discovery example

## 样例简介

本样例将介绍如何在Spring Cloud项目中使用```spring-cloud-starter-tencent-polaris-discovery```以使用其各项功能。

该样例分为两个微服务，即 ```discovery-caller-service``` 和 ```discovery-callee-service ```。
其中 ```discovery-caller-service``` 调用 ```discovery-callee-service```

## 使用说明

### 修改配置

修改 resource/bootstrap.yml 中北极星的服务端地址

```yaml
spring:
  cloud:
    polaris:
      address: grpc://${ip}:8091
```

### 启动样例

#### 启动Polaris后端服务

参考[Polaris Getting Started](https://github.com/PolarisMesh/polaris#getting-started)。

#### 启动应用

- IDEA启动

分别启动

1. ```spring-cloud-tencent-examples/polaris-discovery-example/discovery-caller-service```下的```DiscoveryCallerService```
2. ```spring-cloud-tencent-examples/polaris-discovery-example/discovery-callee-service```下的```DiscoveryCalleeService```

### 验证

#### 调用 discovery-caller-service 暴露的接口

执行以下命令发起Feign调用，其逻辑为```DiscoveryCalleeService```返回 value1+value2 的和

```shell
curl -L -X GET 'http://localhost:48080/discovery/service/caller/feign?value1=1&value2=2'
```

预期返回值

```
3
```

#### RestTemplate调用

执行以下命令发起RestTemplate调用，其逻辑为```DiscoveryCalleeService```返回一段字符串

```shell
curl -L -X GET 'http://localhost:48080/discovery/service/caller/rest'
```

预期返回值

```
Discovery Service Callee
```

