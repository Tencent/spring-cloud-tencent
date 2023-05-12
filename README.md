<img src="https://github.com/Tencent/spring-cloud-tencent/raw/2022.0/doc/logo/rectangle-white.png" width="60%" height="60%"  alt="Spring-Cloud-Tencent-Logo"/>

[![Wiki](https://badgen.net/badge/icon/wiki?icon=wiki&label)](https://github.com/Tencent/spring-cloud-tencent/wiki)
[![Maven Central](https://img.shields.io/maven-central/v/com.tencent.cloud/spring-cloud-tencent?label=Maven%20Central)](https://search.maven.org/search?q=g:com.tencent.cloud%20AND%20a:spring-cloud-tencent)
[![Contributors](https://img.shields.io/github/contributors/Tencent/spring-cloud-tencent)](https://github.com/Tencent/spring-cloud-tencent/graphs/contributors)
[![License](https://img.shields.io/badge/License-BSD%203--Clause-blue.svg)](https://opensource.org/licenses/BSD-3-Clause)

[![Test with Junit 2022.0](https://github.com/Tencent/spring-cloud-tencent/actions/workflows/junit_test17.yml/badge.svg?branch=2022.0)](https://github.com/Tencent/spring-cloud-tencent/actions/workflows/junit_test17.yml)
[![codecov.io](https://codecov.io/gh/Tencent/spring-cloud-tencent/branch/2022.0/graph/badge.svg)](https://codecov.io/gh/Tencent/spring-cloud-tencent?branch=2022.0)

English | [简体中文](./README-zh.md)

README:

- [Introduction](#introduction)
- [How to build](#how-to-build)
- [How to use](#how-to-use)
- [Examples](#examples)

Visit [Wiki](https://github.com/Tencent/spring-cloud-tencent/wiki) to learn more

## Introduction

Spring Cloud Tencent is an one-stop microservice solution which implements the standard Spring Cloud SPI. It integrates
Spring Cloud with Tencent middlewares and makes it easy to develop microservice.

<img src="https://user-images.githubusercontent.com/4991116/170412596-692f8dae-42f7-495f-a451-01396e381eb0.png" width="80%" />

**Service discovery and governance**

Spring Cloud Tencent integrates Spring Cloud with Polaris which is an open source system for service discovery and
governance.

- [Polaris Github](https://github.com/polarismesh/polaris)

Spring Cloud with Polaris can solve these problem:

- service management: service discovery, service registry and health check
- traffic control: customizable routing, load balance, rate limiting and access control
- fault tolerance: circuit breaker for service, interface and instance
- config management: config version control, grayscale release and dynamic update

## How to build

Run these commands to build this project as follow.

**Linux and Mac**

```
./mvnw clean package
```

**Windows**

```
.\mvnw.cmd clean package
```

## How to use

All the components of Spring Cloud Tencent have been uploaded to the Maven central repository, just need to introduce
dependencies.

> Notice:
>
> Support Spring Cloud 2022.0, 2021.0, 2020.0, Hoxton.
>
> The version list of Spring Cloud Tencent can be found
> in
> [Spring Cloud Tencent Version Management](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-%E7%89%88%E6%9C%AC%E7%AE%A1%E7%90%86)
> .

For example:

```` xml  
<!-- add spring-cloud-tencent bom  -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.tencent.cloud</groupId>
            <artifactId>spring-cloud-tencent-dependencies</artifactId>
            <!--version number-->
            <version>1.11.4-2022.0.1</version>
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

## Examples

The experience environment of Polaris is provided for developers:

- [Polaris Console](http://14.116.241.63:8080)
- Polaris Server Address: `grpc://183.47.111.80:8091`

The address of Polaris server in spring-cloud-tencent-example is `grpc://183.47.111.80:8091` by default.

## Chat Group

Please scan the QR code to join the chat group.

<img src="https://user-images.githubusercontent.com/24446200/169198148-d4cc3494-3485-4515-9897-c8cb5504f706.png" width="20%" height="20%" />

## Stargazers over time

If you are interested in Spring Cloud Tencent, please follow our project, thank you very much.

[![Stargazers over time](https://starchart.cc/Tencent/spring-cloud-tencent.svg)](https://starchart.cc/Tencent/spring-cloud-tencent)
