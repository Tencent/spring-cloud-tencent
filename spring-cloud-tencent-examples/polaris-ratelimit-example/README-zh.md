# Spring Cloud Polaris RateLimit Example

## 项目说明

本项目演示如何使用 Polaris ratelimit starter 完成 Spring Cloud 应用的限流管理。

## 示例

### 如何接入

在启动示例进行演示之前，我们先了解一下如何接入 Polaris 限流组件。

> **注意：本章节只是为了便于您理解接入方式，本示例代码中已经完成接入工作，您无需再进行修改。**

1. 首先，修改 `pom.xml` 文件，引入 Polaris ratelimit starter。
    ```xml
    <dependency>
        <groupId>com.tencent.cloud</groupId>
        <artifactId>spring-cloud-starter-tencent-polaris-ratelimit</artifactId>
    </dependency>
    ```

2. 启动应用
    
    北极星提供的example都支持在IDE中直接运行，或者编译打包后通过命令行方式进行运行。
    - 在本地启动Polaris服务端。
    - 在北极星服务端，可以通过控制台，在命名空间Production下，添加服务RateLimitCalleeService。
    - 启动服务被调方：
      1. IDE直接启动：找到主类 `RateLimitCalleeService`，执行 main 方法启动应用。
      2. 打包编译后启动：首先执行 `mvn clean package` 将工程编译打包，然后执行 `java -jar ratelimit-callee-sevice-${verion}.jar`启动应用。  
    - 启动后，可以在北极星控制台上看到注册上来的服务实例信息。

3. 调用服务
    
    通过浏览器访问http://127.0.0.1:48081/business/invoke，可以看到以下输出信息：
    ````
   hello world for ratelimit service 1
   hello world for ratelimit service 2
   hello world for ratelimit service 3
   ...
    ````

4. 配置限流规则并验证
    北极星提供了三个方式进行限流规则的配置（控制台、HTTP接口以及本地文件）。
    
    本示例使用的方式为通过HTTP接口进行配置。通过以下命令来配置：
    ````
   curl -X POST -H "Content-Type:application/json" 127.0.0.1:8090/naming/v1/ratelimits  -d @rule.json
    ````
   
5. 验证限流效果
    继续访问http://127.0.0.1:48081/business/invoke，可以看到，10次调用后，就开始被限流：
    ````
   hello world for ratelimit service 1
   hello world for ratelimit service 2
   ...
   hello world for ratelimit service 10
   request has been limited, service is RateLimitCalleeService, path is /business/invoke, 11
    ````    