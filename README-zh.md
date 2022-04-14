# Spring Cloud Tencent

[![Build Status](https://github.com/Tencent/spring-cloud-tencent/actions/workflows/junit_test.yml/badge.svg)](https://github.com/Tencent/spring-cloud-tencent/actions/workflows/junit_test.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.tencent.cloud/spring-cloud-tencent?label=Maven%20Central)](https://search.maven.org/search?q=g:com.tencent.cloud%20AND%20a:spring-cloud-tencent)

[English](./README.md) | 简体中文 

---

## 介绍

Spring Cloud Tencent 是腾讯开发和维护的一站式微服务解决方案。

Spring Cloud Tencent 实现了Spring Cloud 标准微服务 SPI，开发者可以基于 Spring Cloud Tencent 快速开发 Spring Cloud 云原生分布式应用。

Spring Cloud Tencent 的核心依托腾讯开源的一站式服务发现与治理平台 [Polaris](https://github.com/polarismesh/polaris)，实现各种分布式微服务场景。

- [Polaris Github home page](https://github.com/polarismesh/polaris)
- [Polaris official website](https://polarismesh.cn/)

Spring Cloud 腾讯提供的能力包括但不限于：

- 服务注册和发现
- 动态配置管理
- 服务治理
   - 服务电流限制
   - 服务断路器
   - 服务路由
   - ...
- 标签透传

## 管控台

<img width="1792" alt="image" src="https://user-images.githubusercontent.com/4991116/163402268-48493802-4555-4b93-8e31-011410f2166b.png">

## 使用指南

Spring Cloud Tencent 所有组件都已上传到 Maven 中央仓库，只需要引入依赖即可。

例如：

````  
<!-- add spring-cloud-tencent bom  -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.tencent.cloud</groupId>
            <artifactId>spring-cloud-tencent-dependencies</artifactId>
            <!--version number-->
            <version>${version}</version>
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

- ### 快速开始
    - [Spring Cloud Tencent 版本管理](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Version-Management)
    - [Spring Cloud Tencent 服务注册与发现](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Discovery-Usage-Documentation)
    - [Spring Cloud Tencent 配置中心](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Config-Usage-Documentation)
    - [Spring Cloud Tencent 限流](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Rate-Limit-Usage-Document)
    - [Spring Cloud Tencent 熔断](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Circuitbreaker-Usage-Document)
    - [Spring Cloud Tencent 服务路由](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Router-Usage-Document)
    - [Spring Cloud Tencent 标签传递](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Metadata-Transfer-Usage-Document)

- ### 开发文档
  - [项目概览](https://github.com/Tencent/spring-cloud-tencent/wiki/%E9%A1%B9%E7%9B%AE%E6%A6%82%E8%A7%88)
  - [参与共建](https://github.com/Tencent/spring-cloud-tencent/wiki/Contributing)

## License
The spring-cloud-tencent is licensed under the BSD 3-Clause License. Copyright and license information can be found in the file [LICENSE](LICENSE)
