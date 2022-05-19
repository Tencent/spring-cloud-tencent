# Spring Cloud Tencent Metadata Transfer example

## 样例简介

本样例将介绍如何在Spring Cloud项目中使用```spring-cloud-starter-tencent-metadata-transfer```以使用其各项功能。

本样例包括```metadata-callee-service```、```metadata-caller-service```。

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

### Maven依赖

```xml
<dependency>
    <groupId>com.tencent.cloud</groupId>
    <artifactId>spring-cloud-starter-tencent-metadata-transfer</artifactId>
</dependency>
```

### 启动样例

#### 启动Polaris后端服务

参考[Polaris Getting Started](https://github.com/PolarisMesh/polaris#getting-started)。

#### 启动应用

##### IDEA启动

分别启动
- ```spring-cloud-tencent-examples/metadata-transfer-example/metadata-callee-service```的```MetadataCalleeService```
- ```spring-cloud-tencent-examples/metadata-transfer-example/metadata-caller-service```的```MetadataCallerService```


##### Maven打包启动

在```spring-cloud-tencent-examples/metadata-transfer-example```下执行

```sh
mvn clean package
```

然后在```metadata-callee-service```、```metadata-caller-service```下找到生成的jar包，运行

```
java -jar ${app.jar}
```

启动应用，其中${app.jar}替换为对应的jar包名。

### 元数据配置

在```spring-cloud-tencent-examples/metadata-transfer-example/metadata-caller-service```项目的```bootstrap.yml```配置文件中

```yaml
spring:
  cloud:
    tencent:
      metadata:
        # 定义元数据的键值对
        content:
          # 示例：本地元数据，默认不在链路中传递
          CUSTOM-METADATA-KEY-LOCAL: CUSTOM-VALUE-LOCAL
          # 示例：可传递元数据
          CUSTOM-METADATA-KEY-TRANSITIVE: CUSTOM-VALUE-TRANSITIVE
        # 指定哪个元数据的键值将沿着链接传递
        transitive:
          - CUSTOM-METADATA-KEY-TRANSITIVE

```

### 验证

#### 请求调用

```shell
curl -L -X GET 'http://127.0.0.1:48080/metadata/service/caller/feign/info'
```

预期返回值

```
{
  "caller-metadata-contents": {
    "CUSTOM-METADATA-KEY-TRANSITIVE": "CUSTOM-VALUE-TRANSITIVE",
    "CUSTOM-METADATA-KEY-LOCAL": "CUSTOM-VALUE-LOCAL"
  },
  "callee-transitive-metadata": {
    "CUSTOM-METADATA-KEY-TRANSITIVE": "CUSTOM-VALUE-TRANSITIVE"
  },
  "caller-transitive-metadata": {
    "CUSTOM-METADATA-KEY-TRANSITIVE": "CUSTOM-VALUE-TRANSITIVE"
  }
}
```

返回值解析

- Key `caller-metadata-contents` 表示 `metadata-caller-service` 项目中默认配置的所有的元数据。
- Key `caller-transitive-metadata` 表示 `metadata-caller-service` 项目中指定的可以在链路中传递的元数据列表。
- Key `callee-transitive-metadata` 表示 `metadata-callee-service` 项目被 `metadata-caller-service` 调用时传递过来的上游的元数据列表。

### Wiki参考

查看 [Spring Cloud Tencent Metadata Transfer 使用指南](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Metadata-Transfer-%E4%BD%BF%E7%94%A8%E6%8C%87%E5%8D%97) .