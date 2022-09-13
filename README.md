<img src="https://github.com/Tencent/spring-cloud-tencent/raw/main/doc/logo/rectangle-white.png" width="60%" height="60%"  alt="Spring-Cloud-Tencent-Logo"/>


[![Wiki](https://badgen.net/badge/icon/wiki?icon=wiki&label)](https://github.com/Tencent/spring-cloud-tencent/wiki)
[![Maven Central](https://img.shields.io/maven-central/v/com.tencent.cloud/spring-cloud-tencent?label=Maven%20Central)](https://search.maven.org/search?q=g:com.tencent.cloud%20AND%20a:spring-cloud-tencent)
[![Contributors](https://img.shields.io/github/contributors/Tencent/spring-cloud-tencent)](https://github.com/Tencent/spring-cloud-tencent/graphs/contributors)
[![License](https://img.shields.io/badge/License-BSD%203--Clause-blue.svg)](https://opensource.org/licenses/BSD-3-Clause)

[![Build Status](https://github.com/Tencent/spring-cloud-tencent/actions/workflows/junit_test.yml/badge.svg)](https://github.com/Tencent/spring-cloud-tencent/actions/workflows/junit_test.yml)
[![codecov.io](https://codecov.io/gh/Tencent/spring-cloud-tencent/branch/main/graph/badge.svg)](https://codecov.io/gh/Tencent/spring-cloud-tencent?branch=main)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/Tencent/spring-cloud-tencent.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/Tencent/spring-cloud-tencent/context:java)

English | [简体中文](./README-zh.md)

## Introduction

Spring Cloud Tencent is a open source one-stop microservice solution from Tencent.

Spring Cloud Tencent implements the Spring Cloud standard microservice SPI, so developers can quickly develop Spring
Cloud cloud-native distributed applications based on Spring Cloud Tencent.

The core of Spring Cloud Tencent relies on Tencent's open-source one-stop service discovery and governance
platform [Polaris](https://github.com/polarismesh/polaris) to realize various distributed microservice scenarios.

- [Polaris Github home page](https://github.com/polarismesh/polaris)
- [Polaris official website](https://polarismesh.cn/)

The capabilities provided by Spring Cloud Tencent include but are not limited to:

<img width="1031" alt="image" src="https://user-images.githubusercontent.com/4991116/170412596-692f8dae-42f7-495f-a451-01396e381eb0.png">

- Service registration and discovery
- Dynamic configuration management
- Service Governance
    - Service rate limit
    - Service circuit breaker
    - Service routing
    - ...
- Label transparent transmission

## Demo Environment

- Console Address : http://14.116.241.63:8080/
    - Username: polaris
    - Password: polaris
- Server Address: `grpc://183.47.111.80:8091`

The example addresses under `spring-cloud-tencent-example` all point to the experience service
address (`grpc://183.47.111.80:8091`) by default. If you only experience Spring Cloud Tencent, you can run any example
directly with one click.

## Screenshots

<img width="1792" alt="image" src="https://user-images.githubusercontent.com/4991116/163402268-48493802-4555-4b93-8e31-011410f2166b.png">

## Use Guide

All the components of Spring Cloud Tencent have been uploaded to the Maven central repository, just need to introduce
dependencies.

> Notice:
>
> The version list of Spring Cloud Tencent can be found in [Spring Cloud Tencent Version Management](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-%E7%89%88%E6%9C%AC%E7%AE%A1%E7%90%86).

For example:

```` xml  
<!-- add spring-cloud-tencent bom  -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.tencent.cloud</groupId>
            <artifactId>spring-cloud-tencent-dependencies</artifactId>
            <!--version number-->
            <version>1.7.1-Hoxton.SR12</version>
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

## Develop Guide

You can build this project with command:

```shell
## MacOS or Linux
./mvnw clean package

## Win
.\mvnw.cmd clean package
```

## Documents

- Usage Doc
    - [Spring Cloud Tencent Version Management](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-%E7%89%88%E6%9C%AC%E7%AE%A1%E7%90%86)
    - [Spring Cloud Tencent Discovery](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Discovery-%E4%BD%BF%E7%94%A8%E6%96%87%E6%A1%A3)
    - [Spring Cloud Tencent Config](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Config-%E4%BD%BF%E7%94%A8%E6%96%87%E6%A1%A3)
    - [Spring Cloud Tencent Rate Limit](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Rate-Limit-%E4%BD%BF%E7%94%A8%E6%96%87%E6%A1%A3)
    - [Spring Cloud Tencent CircuitBreaker](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Circuitbreaker-%E4%BD%BF%E7%94%A8%E6%96%87%E6%A1%A3)
    - [Spring Cloud Tencent Router](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Router-%E4%BD%BF%E7%94%A8%E6%96%87%E6%A1%A3)
    - [Spring Cloud Starter Tencent RPC Enhancement](https://github.com/Tencent/spring-cloud-tencent/wiki/RPC%E5%A2%9E%E5%BC%BA)
    - [Spring Cloud Tencent Metadata Transfer](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Metadata-Transfer-%E4%BD%BF%E7%94%A8%E6%8C%87%E5%8D%97)
    - [Actuator Endpoint Extension](https://github.com/Tencent/spring-cloud-tencent/wiki/Actuator-Endpoint-%E6%89%A9%E5%B1%95)

- Best Practices
    - [Multi-feature environment](https://github.com/Tencent/spring-cloud-tencent/wiki/多特性环境)
    - [Multi-registration and multi-discovery](https://github.com/Tencent/spring-cloud-tencent/wiki/Multi-registration-and-multi-discovery)

- Development documentation
    - [Project Structure Overview](https://github.com/Tencent/spring-cloud-tencent/wiki/%E9%A1%B9%E7%9B%AE%E6%A6%82%E8%A7%88)
    - [Participate in co-construction](https://github.com/Tencent/spring-cloud-tencent/wiki/%E5%8F%82%E4%B8%8E%E5%85%B1%E5%BB%BA)

## Chat Group

Please scan the QR code to join the chat group.

<img src="https://user-images.githubusercontent.com/24446200/169198148-d4cc3494-3485-4515-9897-c8cb5504f706.png" width="30%" height="30%" />

## License

The spring-cloud-tencent is licensed under the BSD 3-Clause License. Copyright and license information can be found in
the file [LICENSE](LICENSE)

## Stargazers over time

If you are interested in Spring Cloud Tencent, please follow our project, thank you very much.

[![Stargazers over time](https://starchart.cc/Tencent/spring-cloud-tencent.svg)](https://starchart.cc/Tencent/spring-cloud-tencent)
