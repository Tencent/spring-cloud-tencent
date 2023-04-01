<img src="https://github.com/Tencent/spring-cloud-tencent/raw/2022.0/doc/logo/rectangle-white.png" width="60%" height="60%"  alt="Spring-Cloud-Tencent-Logo"/>

[![Wiki](https://badgen.net/badge/icon/wiki?icon=wiki&label)](https://github.com/Tencent/spring-cloud-tencent/wiki)
[![Maven Central](https://img.shields.io/maven-central/v/com.tencent.cloud/spring-cloud-tencent?label=Maven%20Central)](https://search.maven.org/search?q=g:com.tencent.cloud%20AND%20a:spring-cloud-tencent)
[![Contributors](https://img.shields.io/github/contributors/Tencent/spring-cloud-tencent)](https://github.com/Tencent/spring-cloud-tencent/graphs/contributors)
[![License](https://img.shields.io/badge/License-BSD%203--Clause-blue.svg)](https://opensource.org/licenses/BSD-3-Clause)

[![Test with Junit 2022.0](https://github.com/Tencent/spring-cloud-tencent/actions/workflows/junit_test17.yml/badge.svg?branch=2022.0)](https://github.com/Tencent/spring-cloud-tencent/actions/workflows/junit_test17.yml)
[![codecov.io](https://codecov.io/gh/Tencent/spring-cloud-tencent/branch/2022.0/graph/badge.svg)](https://codecov.io/gh/Tencent/spring-cloud-tencent?branch=2022.0)

[English](./README.md) | 简体中文

---

## 介绍

> 欢迎各位同学点击右上角 Star 按钮，持续关注我们的项目，给予我们最大的支持~

Spring Cloud Tencent 是腾讯开源的一站式微服务解决方案。

Spring Cloud Tencent 实现了Spring Cloud 标准微服务 SPI，开发者可以基于 Spring Cloud Tencent 快速开发 Spring Cloud
云原生分布式应用。

Spring Cloud Tencent 的核心依托腾讯开源的一站式服务发现与治理平台 [PolarisMesh](https://polarismesh.cn/)
，实现各种分布式微服务场景。

- [Polaris Github home page](https://github.com/polarismesh/polaris)
- [Polaris official website](https://polarismesh.cn/)

Spring Cloud Tencent提供的能力包括但不限于：

<img width="1029" alt="image" src="https://user-images.githubusercontent.com/4991116/170412323-ecaf544c-1d7b-45db-9cf0-591544e50c64.png">

- 服务注册和发现
- 动态配置管理
- 服务治理
    - 服务限流
    - 服务熔断
    - 服务路由
    - ...
- 标签透传

## 体验环境

- 管控台地址： http://14.116.241.63:8080/
    - 账号：polaris
    - 密码：polaris
- 控制面地址： `grpc://183.47.111.80:8091`
-

`spring-cloud-tencent-example` 下 example 地址都默认指向了体验服务地址（`grpc://183.47.111.80:8091`），如果您只是体验
Spring Cloud
Tencent，可直接一键运行任何 example。

## 管控台

<img width="1727" alt="sc" src="https://user-images.githubusercontent.com/4991116/197529819-78b20ba8-0e60-450c-a8e3-0c2bf04caa15.png">

## 使用指南

Spring Cloud Tencent 所有组件都已上传到 Maven 中央仓库，只需要引入依赖即可。

> 注意：
>
> 支持Spring Cloud 版本：2022.0、2021.0、2020.0、Hoxton。
>
> Spring Cloud Tencent
> 的版本列表可以查看
> [Spring Cloud Tencent 版本管理](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-%E7%89%88%E6%9C%AC%E7%AE%A1%E7%90%86)
> 。

例如：

```` xml  
<!-- add spring-cloud-tencent bom  -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.tencent.cloud</groupId>
            <artifactId>spring-cloud-tencent-dependencies</artifactId>
            <!--version number-->
            <version>1.10.3-2022.0.1</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>    
                 
<!-- add spring-cloud-starter-tencent-polaris-discovery dependency  -->
<dependencies>
    <dependency>
        <groupId>com.tencent.cloud</groupId>
        <artifactId>spring-cloud-starter-tencent-polaris-discovery</artifactId>
    </dependency>
</dependencies>

````

## 开发入门

You can build this project with command:

```shell
## MacOS or Linux
./mvnw clean package

## Win
.\mvnw.cmd clean package
```

## 文档

请阅读 [Wiki](https://github.com/Tencent/spring-cloud-tencent/wiki)

## 交流群

扫描下面的二维码加入 Spring Cloud Tencent 交流群。

<img src="https://user-images.githubusercontent.com/24446200/169198148-d4cc3494-3485-4515-9897-c8cb5504f706.png" width="30%" height="30%" />

## License

The spring-cloud-tencent is licensed under the BSD 3-Clause License. Copyright and license information can be found in
the file [LICENSE](LICENSE)

## Stargazers over time

如果您对 Spring Cloud Tencent 有兴趣，请关注我们的项目~

[![Stargazers over time](https://starchart.cc/Tencent/spring-cloud-tencent.svg)](https://starchart.cc/Tencent/spring-cloud-tencent)

