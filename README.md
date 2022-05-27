# Spring Cloud Tencent

[![Build Status](https://github.com/Tencent/spring-cloud-tencent/actions/workflows/junit_test.yml/badge.svg)](https://github.com/Tencent/spring-cloud-tencent/actions/workflows/junit_test.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.tencent.cloud/spring-cloud-tencent?label=Maven%20Central)](https://search.maven.org/search?q=g:com.tencent.cloud%20AND%20a:spring-cloud-tencent)
[![codecov.io](https://codecov.io/gh/Tencent/spring-cloud-tencent/branch/main/graph/badge.svg)](https://codecov.io/gh/Tencent/spring-cloud-tencent?branch=main)
[![Contributors](https://img.shields.io/github/contributors/Tencent/spring-cloud-tencent)](https://github.com/Tencent/spring-cloud-tencent/graphs/contributors)
[![License](https://img.shields.io/badge/License-BSD%203--Clause-blue.svg)](https://opensource.org/licenses/BSD-3-Clause)
[![Percentage of issues still open](http://isitmaintained.com/badge/open/Tencent/spring-cloud-tencent.svg)](https://github.com/Tencent/spring-cloud-tencent/issues)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-brightgreen.svg)](https://github.com/Tencent/spring-cloud-tencent/wiki/Contributing)

English | [简体中文](./README-zh.md)

## Introduction

Spring Cloud Tencent is a open source one-stop microservice solution from Tencent.

Spring Cloud Tencent implements the Spring Cloud standard microservice SPI, so developers can quickly develop Spring Cloud cloud-native distributed applications based on Spring Cloud Tencent.

The core of Spring Cloud Tencent relies on Tencent's open-source one-stop service discovery and governance platform [Polaris](https://github.com/polarismesh/polaris) to realize various distributed microservice scenarios.

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

## Screenshots

<img width="1792" alt="image" src="https://user-images.githubusercontent.com/4991116/163402268-48493802-4555-4b93-8e31-011410f2166b.png">

## Use Guide

All the components of Spring Cloud Tencent have been uploaded to the Maven central repository, just need to introduce dependencies.

For example:

```` xml  
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

 - ### Quick Start
    - [Spring Cloud Tencent Version Management](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Version-Management)
    - [Spring Cloud Tencent Discovery](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Discovery-Usage-Documentation)
    - [Spring Cloud Tencent Config](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Config-Usage-Documentation)
    - [Spring Cloud Tencent Rate Limit](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Rate-Limit-Usage-Document)
    - [Spring Cloud Tencent CircuitBreaker](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Circuitbreaker-Usage-Document)
    - [Spring Cloud Tencent Router](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Router-Usage-Document)
    - [Spring Cloud Tencent Metadata Transfer](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Metadata-Transfer-Usage-Document)

- ### Development Documentation
  - [Project Structure Overview](https://github.com/Tencent/spring-cloud-tencent/wiki/%E9%A1%B9%E7%9B%AE%E6%A6%82%E8%A7%88)
  - [Participate in co-construction](https://github.com/Tencent/spring-cloud-tencent/wiki/Contributing)
  
## Chat Group

Please scan the QR code to join the chat group.

<img src="https://user-images.githubusercontent.com/24446200/169198148-d4cc3494-3485-4515-9897-c8cb5504f706.png" width="30%" height="30%" />

## License
The spring-cloud-tencent is licensed under the BSD 3-Clause License. Copyright and license information can be found in the file [LICENSE](LICENSE)


## Stargazers over time

If you are interested in Spring Cloud Tencent, please follow our project, thank you very much.

[![Stargazers over time](https://starchart.cc/Tencent/spring-cloud-tencent.svg)](https://starchart.cc/Tencent/spring-cloud-tencent)
