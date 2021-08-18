# Spring Cloud Polaris RateLimit Example

## Project Explanation

This project shows how to use ratelimit feature of Polaris to complete Spring Cloud application's rate limit

## Example

### How to access

Before showcasing the project, let's get to know how to access Polaris rate limit component

> ** note: this chapter is to help you understand different ways to access, the codes in the example has been executed, you don't need to re-edit.**

1, first, change document `pom.xml`, introduce Polaris ratelimit starter

  ```xml
  <dependency>
      <groupId>com.tencent.cloud</groupId>
      <artifactId>spring-cloud-starter-tencent-polaris-ratelimit</artifactId>
  </dependency>
  ```

2. Launch Application

Examples provided by Polaris all support to run at IDE, or compile and run with orders

- Launch Polaris locally

- at Polaris end, through control panel, under namespace Product, add RateLimitCalleeService

- Launch callee server:

   1. Launch IDE directly: First find `RateLimitCalleeService`, execute main then launch application
   2. compile package then launch: first execute `mvn clean package` compile the package, then execute  `java -jar ratelimit-callee-sevice-${verion}.jar` execute the application

   - After launching, one can watch server instance from Polaris control panel

   3. Invoke Service

  After visiting http://127.0.0.1:48081/business/invoke, one can see the following information:

   ````
  hello world for ratelimit service 1
  hello world for ratelimit service 2
  hello world for ratelimit service 3
  ...
   ````

4. Configuration rate limit and verification
   Polaris provide three wats to conduct rate limit configuration (control panel, HTTP port and local files)

this example is HTTP configuration. One can figure with the following steps:

 ````
  curl -X POST -H "Content-Type:application/json" 127.0.0.1:8090/naming/v1/ratelimits  -d @rule.json
 ````

5. Verify rate limit result
   continue visit http://127.0.0.1:48081/business/invoke, one can see, after 10 invokes, rate limit will start:

  ````
   hello world for ratelimit service 1
   hello world for ratelimit service 2

  ````