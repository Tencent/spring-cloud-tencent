# Spring Cloud Polaris Discovery example

## 样例简介

本样例将介绍如何在Spring Cloud项目中使用```spring-cloud-starter-tencent-polaris-discovery```以使用其各项功能。

该样例分为两个微服务，即discovery-caller-service和discovery-callee-service。其中，discovery-caller-service对discovery-callee-service发生调用。

## 使用说明

### 修改配置

配置如下所示。其中，${ip}和${port}为Polaris后端服务的IP地址与端口号。

```yaml
spring:
  application:
    name: ${application.name}
  cloud:
    polaris:
      address: ${ip}:${port}
```

### 启动样例

#### 启动Polaris后端服务

参考[Polaris Getting Started](https://github.com/PolarisMesh/polaris#getting-started)。

#### 启动应用

- IDEA启动

分别启动```spring-cloud-tencent-examples/polaris-discovery-example/discovery-caller-service```下的```DiscoveryCallerService```和```spring-cloud-tencent-examples/polaris-discovery-example/discovery-callee-service```下的```DiscoveryCalleeService```。

- Maven打包启动

在```spring-cloud-tencent-examples/polaris-discovery-example```下执行

```sh
mvn clean package
```

然后在```discovery-caller-service```和```discovery-callee-service```下找到生成的jar包，运行

```
java -jar ${app.jar}
```

启动应用，其中${app.jar}替换为对应的jar包名。

### 验证

#### Feign调用

执行以下命令发起Feign调用，其逻辑为```DiscoveryCalleeService```返回value1+value2的和

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

