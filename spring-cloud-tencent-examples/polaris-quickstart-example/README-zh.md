# Spring Cloud Polaris Quickstart example

[English](./README.md) | 简体中文 

---

## 样例简介

本样例将介绍如何使应用快速接入Spring Cloud Tencent。

## 使用说明

### 修改配置

在 ```polaris-quickstart-example/quickstart-provider``` 以及 ```polaris-quickstart-example/quickstart-consumer``` 两个项目中，修改```bootstrap.yml```，修改后配置如下所示。
其中，```${ip}```和${port}为Polaris后端服务的IP地址与端口号。

```yaml
spring:
  application:
    name: EchoService
  cloud:
    polaris:
      address: grpc://${ip}:${port}
```

### 启动样例

#### 启动Provider

- IDEA启动

 找到 ```polaris-quickstart-example/quickstart-provider``` 项目的主类 ```EchoServerApplication```，执行 main 方法启动样例。

- Maven打包启动

在```polaris-quickstart-example/quickstart-provider```下执行

```sh
mvn clean package
```

然后找到生成的jar包，运行

```
java -jar ${app.jar}
```

启动应用，其中`${app.jar}`替换为对应的jar包名。

#### 启动Consumer

- IDEA启动

 找到 ```polaris-quickstart-example/quickstart-consumer``` 项目的主类 ```EchoClientApplication```，执行 main 方法启动样例。

- Maven打包启动

在```polaris-quickstart-example/quickstart-consumer```下执行

```sh
mvn clean package
```

然后找到生成的jar包，运行

```
java -jar ${app.jar}
```

启动应用，其中`${app.jar}`替换为对应的jar包名。

### 验证

#### HTTP调用

Consumer 和 Provider 启动端口都是随机生成的，因此需要记录Consumer启动时候的端口：
```
11:26:53 [main] INFO  o.s.b.w.e.tomcat.TomcatWebServer - Tomcat started on port(s): 58838 (http) with context path ''
```
执行http调用，其中`${app.port}`替换为启动的端口。
```shell
curl -L -X GET 'http://localhost:${app.port}/echo?value=hello_world''
```

预期返回值：`echo: hello_world`

