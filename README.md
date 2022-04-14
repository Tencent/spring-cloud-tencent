# Spring Cloud Tencent

[![Build Status](https://github.com/Tencent/spring-cloud-tencent/actions/workflows/junit_test.yml/badge.svg)](https://github.com/Tencent/spring-cloud-tencent/actions/workflows/junit_test.yml)
[![Maven Central](https://img.shields.io/maven-central/v/com.tencent.cloud/spring-cloud-tencent?label=Maven%20Central)](https://search.maven.org/search?q=g:com.tencent.cloud%20AND%20a:spring-cloud-tencent)

English | [简体中文](./README-zh.md)

## Introduction

Spring Cloud Tencent is a one-stop microservice solution developed and maintained by Tencent.

Spring Cloud Tencent implements the Spring Cloud standard microservice SPI, so developers can quickly develop Spring Cloud cloud-native distributed applications based on Spring Cloud Tencent.

The core of Spring Cloud Tencent relies on Tencent's open-source one-stop service discovery and governance platform [Polaris](https://github.com/polarismesh/polaris) to realize various distributed microservice scenarios.

- [Polaris Github home page](https://github.com/polarismesh/polaris)
- [Polaris official website](https://polarismesh.cn/)

The capabilities provided by Spring Cloud Tencent include but are not limited to:

- Service registration and discovery
- Dynamic configuration management
- Service Governance
  - Service current limit
  - Service circuit breaker
  - Service routing
  - ...
- Label transparent transmission

## Use Guide

All the components of Spring Cloud Tencent have been uploaded to the Maven central repository, just need to introduce dependencies.

For example:

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

 - Quick Start
    - [Spring Cloud Tencent Version Management](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Version-Management)
    - [Spring Cloud Tencent Discovery](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Discovery-Usage-Documentation)
    - [Spring Cloud Tencent Config](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Config-Usage-Documentation)
    - [Spring Cloud Tencent Rate Limit](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Rate-Limit-Usage-Document)
    - [Spring Cloud Tencent CircuitBreaker](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Circuitbreaker-Usage-Document)
    - [Spring Cloud Tencent Router](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Router-Usage-Document)
    - [Spring Cloud Tencent Metadata Transfer](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Metadata-Transfer-Usage-Document)

- Development Documentation
  - [Project Structure Overview](https://github.com/Tencent/spring-cloud-tencent/wiki/%E9%A1%B9%E7%9B%AE%E6%A6%82%E8%A7%88)
  - [Participate in co-construction](https://github.com/Tencent/spring-cloud-tencent/wiki/Contributing)
  
## License
The spring-cloud-tencent is licensed under the BSD 3-Clause License. Copyright and license information can be found in the file [LICENSE](LICENSE)
