# Spring Cloud Tencent

[![Wiki](https://badgen.net/badge/icon/wiki?icon=wiki&label)](https://github.com/Tencent/spring-cloud-tencent/wiki)
[![Maven Central](https://img.shields.io/maven-central/v/com.tencent.cloud/spring-cloud-tencent?label=Maven%20Central)](https://search.maven.org/search?q=g:com.tencent.cloud%20AND%20a:spring-cloud-tencent)
[![Contributors](https://img.shields.io/github/contributors/Tencent/spring-cloud-tencent)](https://github.com/Tencent/spring-cloud-tencent/graphs/contributors)
[![License](https://img.shields.io/badge/License-BSD%203--Clause-blue.svg)](https://opensource.org/licenses/BSD-3-Clause)

[![Test with Junit](https://github.com/Tencent/spring-cloud-tencent/actions/workflows/junit_test.yml/badge.svg?branch=2021.0)](https://github.com/Tencent/spring-cloud-tencent/actions/workflows/junit_test.yml)
[![codecov.io](https://codecov.io/gh/Tencent/spring-cloud-tencent/branch/2021.0/graph/badge.svg)](https://codecov.io/gh/Tencent/spring-cloud-tencent?branch=2021.0)

[English](./README.md) | 简体中文

README:

- [介绍](#介绍)
- [如何构建](#如何构建)
- [如何使用](#如何使用)
- [使用示例](#使用示例)

更多文档请查看[Wiki](https://github.com/Tencent/spring-cloud-tencent/wiki)

## 介绍

Spring Cloud Tencent 是基于 Spring Cloud SPI 实现的一站式微服务解决方案。通过集成 Spring Cloud
和腾讯中间件，让分布式服务和微服务的开发、测试、发布和运维变得更加简单。

<img src="https://user-images.githubusercontent.com/4991116/170412323-ecaf544c-1d7b-45db-9cf0-591544e50c64.png" width="80%" />

**服务发现和治理**

Spring Cloud Tencent 集成 Spring Cloud 和北极星。北极星是一个支持多语言和多框架的服务发现和治理平台。

- [北极星 Github](https://github.com/polarismesh/polaris)

Spring Cloud 集成北极星可以解决以下问题：

- 服务管理：服务发现、服务注册、健康检查
- 流量控制：可自定义的流量路由、负载均衡、限频限流、访问控制
- 故障容错：服务和接口熔断和降级、实例熔断和切换
- 配置管理：版本管理、灰度发布、动态更新

## 如何构建

运行以下命令进行构建。

**Linux and Mac**

```
./mvnw clean package
```

**Windows**

```
.\mvnw.cmd clean package
```

## 如何使用

Spring Cloud Tencent 所有组件都已上传到 Maven 中央仓库，只需要引入依赖即可。

> 注意：
>
> 支持Spring Cloud 版本：2023.0, 2022.0、2021.0、2020.0、Hoxton。
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
            <version>${LATEST_VERSION_FROM_VERSION_MANAGEMENT_IN_WIKI}</version>
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

## 使用示例

北极星为开发者提供体验环境：

- [北极星控制台](http://119.91.66.223:80)
- 北极星服务端地址：`grpc://119.91.66.223:8091`

在 spring-cloud-tencent-example 项目里，北极星服务端地址默认配置为`grpc://119.91.66.223:8091`。

## 交流群

请扫描下方二维码添加微信，并发送“Spring Cloud Tencent”申请加群。

<img src="https://github.com/Tencent/spring-cloud-tencent/assets/24446200/77912670-aa7b-44ec-a312-42a05d59b109" width=20% height=20%>

## Stargazers over time

[![](https://starchart.cc/Tencent/spring-cloud-tencent.svg)](https://starchart.cc/Tencent/spring-cloud-tencent)
