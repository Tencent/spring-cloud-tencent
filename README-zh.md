# Spring Cloud Tencent

[English](./README.md) | 简体中文 

---

## 介绍

Spring Cloud Tencent包含了分布式应用微服务开发过程中所需的组件，基于 Spring Cloud 框架的开发者可以使用这些组件快速进行分布式应用的开发。

在Spring Cloud Tencent的基础上，您只需要添加少量配置，就可以将 Spring Cloud 应用接入腾讯云微服务解决方案，通过腾讯云中间件来迅速搭建分布式应用系统。

## 主要功能

* **服务注册与发现**：基于 Spring Cloud Common的标准进行微服务的注册与发现。
* **服务路由与负载均衡**：基于 Ribbon 的接口标准，提供场景更丰富的动态路由以及负载均衡的能力。
* **故障节点熔断**：提供故障节点的熔断剔除以及主/被动探测恢复的能力，保证分布式服务的可靠性。
* **服务限流**：支持微服务被调接入层和网关主动调用的限流功能，保证后台微服务稳定性，可通过控制台动态配置规则，及查看流量监控数据。
* **元数据传递**：支持网关和微服务之间的元数据传递。

## 组件

**[Polaris](https://github.com/PolarisMesh/polaris)**：北极星云原生的服务治理平台，解决远程调用的服务注册发现、动态路由、负载均衡和容错问题。

## 如何构建

* master 分支对应的是 Spring Cloud Hoxton，最低支持 JDK 1.8。

Spring Cloud Tencent 使用 Maven 来构建，最快的使用方式是将本项目 clone 到本地，然后执行以下命令：
```bash
	./mvnw install
```
执行完毕后，项目将被安装到本地 Maven 仓库。

## 如何使用

### 如何引入依赖

在 dependencyManagement 中添加如下配置，然后在 dependencies 中添加自己所需使用的依赖即可使用。

````
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.tencent.cloud</groupId>
                <artifactId>spring-cloud-tencent-dependencies</artifactId>
                <!--版本号需修改成实际依赖的版本号-->
                <version>${version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
````

### 示例

Spring Cloud Tencent 项目包含了一个子模块spring-cloud-tencent-examples。此模块中提供了体验接入用的 example ，您可以阅读对应的 example 工程下的 readme 文档，根据里面的步骤来体验。

Example 列表：

- [Polaris Discovery Example](spring-cloud-tencent-examples/polaris-discovery-example/README-zh.md)

- [Polaris CircuitBreaker Example](spring-cloud-tencent-examples/polaris-circuitbreaker-example/README-zh.md)

- [Polaris RateLimit Example](spring-cloud-tencent-examples/polaris-ratelimit-example/README-zh.md)

- [Polaris Gateway Example](spring-cloud-tencent-examples/polaris-gateway-example/README-zh.md)

## 版本号规范

采取与Spring Cloud大版本号相关的版本策略。

项目的版本号格式为 ```大版本号.小版本号.补丁版本号.对应Spring Cloud的大版本号.对应Spring Cloud的小版本号-发布类型``` 的形式。
大版本号、小版本号、补丁版本号的类型为数字，从 0 开始取值。
对应Spring Cloud的大版本号为Spring Cloud提供的英文版本号，例如Hoxton、Greenwich等。对应Spring Cloud的小版本号为Spring Cloud给出的小版本号，例如 RS9 等。
发布类型目前包括正式发布和发布候选版（RC）。在实际的版本号中，正式发布版不额外添加发布类型，发布候选版将添加后缀，并从 RC0 开始。

示例：1.0.1.Hoxton.SR9-RC0

## License
The spring-cloud-tencent is licensed under the BSD 3-Clause License. Copyright and license information can be found in the file [LICENSE](LICENSE)
