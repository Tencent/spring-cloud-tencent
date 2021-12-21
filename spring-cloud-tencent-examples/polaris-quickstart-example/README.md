# Spring Cloud Polaris QuickStart example

English | [简体中文](./README-zh.md)

---

## Introduction

This example shows how to make application integrated with spring-cloud-tencent rapidly.

## Instruction

### Configuration

Modify ```bootstrap.yml```, ${ip} and ${port} is the address of polaris server.

```yaml
spring:
  application:
    name: EchoService
  cloud:
    polaris:
      address: grpc://${ip}:${port}
```

### Start Application

#### Start Provider

- Start in IDEA

Find main class ```EchoServerApplication``` in project ```polaris-quickstart-example/quickstart-provider```, and execute the main method.

- Start by fatjar

Run build command in ```polaris-quickstart-example/quickstart-provider```:

```sh
mvn clean package
```

find the generated fatjar, run:

```
java -jar ${app.jar}
```

${app.jar} replace to the built jar name.

#### Start Consumer

- Start in IDEA

Find main class ```EchoClientApplication``` in project ```polaris-quickstart-example/quickstart-consumer```, and execute the main method.

- Start by fatjar

Run build command in ```polaris-quickstart-example/quickstart-consumer```:

```sh
mvn clean package
```

find the generated fatjar, run:

```
java -jar ${app.jar}
```

${app.jar} replace to the built jar name.

### Verify

#### Invoke by http call

Consumer and Provider application use random generated port, so you need to record the consumer port from start log.
 ```
 11:26:53 [main] INFO  o.s.b.w.e.tomcat.TomcatWebServer - Tomcat started on port(s): 58838 (http) with context path ''
 ```
Invoke http call，replace `${app.port}` to the consumer port.
```shell
curl -L -X GET 'http://localhost:47080/quickstart/feign?msg=hello_world''
```

expect：`echo: hello_world`