# Spring Cloud Polaris CircuitBreaker Example

## 样例简介

本样例将介绍如何在Spring Cloud项目中使用```spring-cloud-starter-tencent-polaris-circuitbreaker```以使用其各项功能。

该样例分为两个微服务，即polaris-circuitbreaker-example-a和polaris-circuitbreaker-example-b。其中，polaris-circuitbreaker-example-a对polaris-circuitbreaker-example-b发生调用。

## 使用说明

### 修改配置

在两个微服务的```src/main/resources```下的```bootstrap.yml```文件中添加如下配置。其中，${ip}和${port}为Polaris后端服务的IP地址与端口号。

```yaml
spring:
  application:
    name: ${application.name}
  cloud:
    polaris:
      server-addr: ${ip}:${port}
```

### 启动样例

#### 启动Polaris后端服务

参考[Polaris](https://github.com/polarismesh)。

#### 启动应用

注意，由于需要验证熔断功能，因此需要部署两个及以上的被调服务（样例中部署两个即可）。
- IDEA启动

分别启动```spring-cloud-tencent-examples/polaris-circuitbreaker-example/polaris-circuitbreaker-example-a```下的```ServiceA```和```spring-cloud-tencent-examples/polaris-circuitbreaker-example/polaris-circuitbreaker-example-b```下的```ServiceB```。

注意，ServiceB需要启动两个。同机器上可以修改端口号来实现。

两个ServiceB的com.tencent.cloud.polaris.circuitbreaker.example.ServiceBController.info的逻辑需不同，即一个正常返回一个抛出异常。

- Maven打包启动

在```spring-cloud-tencent-examples/polaris-discovery-example```下执行

注意，ServiceB需要启动两个。同机器上可以修改端口号来实现。

两个ServiceB的com.tencent.cloud.polaris.circuitbreaker.example.ServiceBController.info的逻辑需不同，即一个正常返回一个抛出异常。

```sh
mvn clean package
```

然后在```polaris-circuitbreaker-example-a```和```polaris-circuitbreaker-example-b```下找到生成的jar包，运行

```
java -jar ${app.jar}
```

启动应用，其中${app.jar}替换为对应的jar包名。

### 验证

#### Feign调用

执行以下命令发起Feign调用，其逻辑为```ServiceB```抛出一个异常

```shell
curl -L -X GET 'localhost:48080/example/service/a/getBServiceInfo'
```

预期返回情况：

在出现
```
trigger the refuse for service b
```
时，表示请求到有异常的ServiceB，需要熔断这个实例。后面的所有请求都会得到正常返回。

