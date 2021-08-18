# Spring Cloud Polaris Discovery example

## Example Introduction

This example shows how to use ```spring-cloud-starter-tencent-polaris-discovery`` in Spring Cloud project for its features.

This example is divided to two microservice, discovery-caller-service and discovery-callee-service. In these two microservices, discovery-caller-service invokes discovery-callee-service.

## Instruction

### Configuration

The configuration is as the following shows. ${ip} and ${port} are Polaris backend IP address and port number.

```yaml
spring:
  application:
    name: ${application.name}
  cloud:
    polaris:
      address: ${ip}:${port}
```

### Launching Example

#### Launching Polaris Backend Service

Reference to [Polaris Getting Started](https://github.com/PolarisMesh/polaris#getting-started)

#### Launching Application

- IDEA Launching

Launching ```spring-cloud-tencent-examples/polaris-discovery-example/discovery-caller-service```'s  ```DiscoveryCallerService``` and ```spring-cloud-tencent-examples/polaris-discovery-example/discovery-callee-service```'s ```DiscoveryCalleeService```

- Maven Package Launching

Execute under ```spring-cloud-tencent-examples/polaris-discovery-example```

```sh
mvn clean package
```

Then at ```discovery-caller-service``` and ```discovery-callee-service``` find the package that generates jar, and run it

```
java -jar ${app.jar}
```

Launch application, change ${app.jar} to jar's package name

### Verify

#### Feign Invoke

Execute the following orders to invoke Feign, ```DiscoveryCalleeService``` goes bank to the sum of value1+value2

```shell
curl -L -X GET 'http://localhost:48080/discovery/service/caller/feign?value1=1&value2=2'
```

Expected return rate

```
3
```

#### RestTemplate Invoke

Execute the following orders to invoke RestTemplate, ```DiscoveryCalleeService``` goes back to string characters

```shell
curl -L -X GET 'http://localhost:48080/discovery/service/caller/rest'
```

Expected return rate

```
Discovery Service Callee
```

