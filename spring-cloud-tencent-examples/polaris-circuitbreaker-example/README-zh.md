# Spring Cloud Polaris Circuitbreaker example

## 样例简介

本样例将介绍如何在Spring Cloud项目中使用```spring-cloud-starter-tencent-polaris-circuitbreaker```以使用其各项功能。

本样例包括被调方```polaris-circuitbreaker-callee-service```、```polaris-circuitbreaker-callee-service2```和主调方```polaris-circuitbreaker-feign-example```、```polaris-circuitbreaker-gateway-example```、```polaris-circuitbreaker-webclient-example```。

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

#### 启动被调应用

分别启动```polaris-circuitbreaker-example/polaris-circuitbreaker-callee-service```、```polaris-circuitbreaker-example/polaris-circuitbreaker-callee-service2```


#### 启动主调应用

##### 启动Feign并验证

启动```polaris-circuitbreaker-example/polaris-circuitbreaker-feign-example```。

发送请求`curl --location --request GET 'http://127.0.0.1:48080/example/service/a/getBServiceInfo/fallbackFromPolaris'`, 验证熔断和Polaris-server远程拉取降级。

发送请求`curl --location --request GET 'http://127.0.0.1:48080/example/service/a/getBServiceInfo/fallbackFromCode'`, 验证熔断和代码降级。

##### 启动RestTemplate并验证

启动```polaris-circuitbreaker-example/polaris-circuitbreaker-resttemplate-example```。

发送请求`curl --location --request GET 'http://127.0.0.1:48080/example/service/a/getBServiceInfo/fallbackFromPolaris'`, 验证熔断和Polaris-server远程拉取降级。

发送请求`curl --location --request GET 'http://127.0.0.1:48080/example/service/a/getBServiceInfo/fallbackFromCode'`, 验证熔断和代码降级。

##### 启动WebClient并验证

启动```polaris-circuitbreaker-example/polaris-circuitbreaker-webclient-example```。

发送请求`curl --location --request GET 'http://127.0.0.1:48080/example/service/a/getBServiceInfo'`, 验证熔断和代码降级。

##### 启动SCG并验证

启动```polaris-circuitbreaker-example/polaris-circuitbreaker-gateway-example```。

发送请求`curl --location --request GET 'http://127.0.0.1:48080/polaris-circuitbreaker-callee-service/example/service/b/info'`, 验证熔断和代码降级。

修改```polaris-circuitbreaker-example/polaris-circuitbreaker-gateway-example/resources/bootstrap.yml```。删除本地fallback方法并重启，验证熔断和Polaris-server远程拉取降级。

