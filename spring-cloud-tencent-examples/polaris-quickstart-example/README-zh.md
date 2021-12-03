# Spring Cloud Polaris Quickstart example

## 样例简介

本样例将介绍如何使应用快速接入Spring Cloud Tencent。

## 使用说明

### 修改配置

修改bootstrap.yml，修改后配置如下所示。其中，${ip}和${port}为Polaris后端服务的IP地址与端口号。

```yaml
spring:
  application:
    name: EchoService
  cloud:
    polaris:
      address: grpc://${ip}:${port}
```

### 启动样例

#### 启动应用

- IDEA启动

找到 polaris-quickstart-example 项目的主类 EchoServiceApplication，执行 main 方法启动样例。

- Maven打包启动

在```spring-cloud-tencent-examples/polaris-quickstart-example```下执行

```sh
mvn clean package
```

然后找到生成的jar包，运行

```
java -jar ${app.jar}
```

启动应用，其中${app.jar}替换为对应的jar包名。

### 验证

#### HTTP调用

```shell
curl -L -X GET 'http://localhost:47080/quickstart/feign?msg=hello_world''
```

预期返回值：hello_world