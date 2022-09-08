# Spring Cloud Tencent Metadata Transfer example

## 样例简介

本样例将介绍如何在Spring Cloud项目中使用```spring-cloud-starter-tencent-metadata-transfer```以使用其各项功能。

本样例包括```metadata-frontend```、```metadata-middle```、```metadata-backend```。

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

- ```spring-cloud-tencent-examples/metadata-transfer-example/metadata-frontend```的```MetadataFrontendService```
- ```spring-cloud-tencent-examples/metadata-transfer-example/metadata-middle```的```MetadataMiddleService```
- ```spring-cloud-tencent-examples/metadata-transfer-example/metadata-backend```的```MetadataBackendService```

##### Maven打包启动

在```spring-cloud-tencent-examples/metadata-transfer-example```下执行

```sh
mvn clean package
```

然后在```metadata-frontend```、```metadata-middle```、```metadata-backend```下找到生成的jar包，运行

```
java -jar ${app.jar}
```

启动应用，其中${app.jar}替换为对应的jar包名。

### 元数据配置

- 在```spring-cloud-tencent-examples/metadata-transfer-example/metadata-frontend```项目的```bootstrap.yml```配置文件中

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
          CUSTOM-METADATA-KEY-TRANSITIVE: CUSTOM-VALUE-TRANSITIVE-FRONTEND
          # 示例：一次性元数据
          CUSTOM-METADATA-KEY-DISPOSABLE: CUSTOM-VALUE-DISPOSABLE-FRONTEND
        # 指定哪个元数据的键值将沿着链接传递
        transitive:
          - CUSTOM-METADATA-KEY-TRANSITIVE
        # 指定哪个元数据的键值只进行一次性传递（一跳）
        disposable:
          - CUSTOM-METADATA-KEY-DISPOSABLE
```

- 在```spring-cloud-tencent-examples/metadata-transfer-example/metadata-frontend```项目的```bootstrap.yml```配置文件中

```yaml
spring:
  cloud:
    tencent:
      metadata:
        # 定义元数据的键值对
        content:
          # 示例：本地元数据，默认不在链路中传递
          CUSTOM-METADATA-KEY-LOCAL-2: CUSTOM-VALUE-LOCAL-2
          # 示例：可传递元数据
          CUSTOM-METADATA-KEY-TRANSITIVE-2: CUSTOM-VALUE-TRANSITIVE-2
          # 示例：一次性元数据
          CUSTOM-METADATA-KEY-DISPOSABLE: CUSTOM-VALUE-DISPOSABLE-MIDDLE
        # 指定哪个元数据的键值将沿着链接传递
        transitive:
          - CUSTOM-METADATA-KEY-TRANSITIVE-2
        # 指定哪个元数据的键值只进行一次性传递（一跳）
        disposable:
          - CUSTOM-METADATA-KEY-DISPOSABLE
```

### 验证

#### 请求调用

```shell
curl -L -X GET 'http://127.0.0.1:48080/metadata/service/frontend/feign/info'
```

预期返回值

```json
{
  "frontend-transitive-metadata": {
    "CUSTOM-METADATA-KEY-TRANSITIVE": "CUSTOM-VALUE-TRANSITIVE-FRONTEND"
  },
  "frontend-upstream-disposable-metadata": {
  },
  "frontend-local-disposable-metadata": {
    "CUSTOM-METADATA-KEY-DISPOSABLE": "CUSTOM-VALUE-DISPOSABLE-FRONTEND"
  },
  "middle-transitive-metadata": {
    "CUSTOM-METADATA-KEY-TRANSITIVE": "CUSTOM-VALUE-TRANSITIVE-FRONTEND",
    "CUSTOM-METADATA-KEY-TRANSITIVE-2": "CUSTOM-VALUE-TRANSITIVE-2"
  },
  "middle-upstream-disposable-metadata": {
    "CUSTOM-METADATA-KEY-DISPOSABLE": "CUSTOM-VALUE-DISPOSABLE-FRONTEND"
  },
  "middle-local-disposable-metadata": {
    "CUSTOM-METADATA-KEY-DISPOSABLE": "CUSTOM-VALUE-DISPOSABLE-MIDDLE"
  },
  "backend-transitive-metadata": {
    "CUSTOM-METADATA-KEY-TRANSITIVE": "CUSTOM-VALUE-TRANSITIVE-FRONTEND",
    "CUSTOM-METADATA-KEY-TRANSITIVE-2": "CUSTOM-VALUE-TRANSITIVE-2"
  },
  "backend-upstream-disposable-metadata": {
    "CUSTOM-METADATA-KEY-DISPOSABLE": "CUSTOM-VALUE-DISPOSABLE-MIDDLE"
  },
  "backend-local-disposable-metadata": {
  }
}
```

返回值解析

> `*`(星号)，代表示例中的`frontend`、`middle`、`backend`。

- Key `*-transitive-metadata` 表示服务中默认配置的所有的可传递（全链路）的元数据。
- Key `*-upstream-disposable-metadata` 表示服务中从上游请求中获取到的一次性传递的元数据。
- Key `*-local-disposable-metadata` 表示当前服务配置的往下游传递的一次性的元数据。

### 如何通过Api获取传递的元数据

- 获取全局传递的元数据

```
MetadataContext context=MetadataContextHolder.get();
Map<String, String> customMetadataMap=context.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);
customMetadataMap.forEach((key,value)->{
	// ...
});
```

- 获取上游传递过来的一次性元数据

```
Map<String, String> upstreamDisposableMetadatas=MetadataContextHolder.getAllDisposableMetadata(true);
upstreamDisposableMetadatas.forEach((key,value)->{
	// ...
});
```

- 获取本地配置的一次性元数据

```
Map<String, String> localDisposableMetadatas=MetadataContextHolder.getAllDisposableMetadata(false);
localDisposableMetadatas.forEach((key,value)->{
	// ...
});
```

### Wiki参考

查看 [Spring Cloud Tencent Metadata Transfer 使用指南](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Metadata-Transfer-%E4%BD%BF%E7%94%A8%E6%8C%87%E5%8D%97)。