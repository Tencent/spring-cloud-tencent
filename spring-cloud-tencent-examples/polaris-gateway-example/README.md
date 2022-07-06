# Spring Cloud Polaris Gateway example

## Example Introduction

This example shows how to use ```spring-cloud-tencent-polaris-gateway``` in Spring Cloud project for its features.

This example contains ```gateway-zuul-service```, ```gateway-scg-service``` and ```gateway-callee-service```. ```gateway-zuul-service``` and ```gateway-scg-service``` invoke ```gateway-callee-service```.

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

Launching ```spring-cloud-tencent-examples/polaris-gateway-example/gateway-zuul-service```'s  ```GatewayZuulService```,  ```spring-cloud-tencent-examples/polaris-gateway-example/gateway-scg-service```'s ```GatewayScgService``` and ```spring-cloud-tencent-examples/polaris-gateway-example/gateway-callee-service```'s ```GatewayCalleeService```

- Maven Package Launching

Execute under ```spring-cloud-tencent-examples/polaris-gateway-example```

```sh
mvn clean package
```

Then find the jars under ```gateway-zuul-service```, ```gateway-scg-service``` and ```gateway-callee-service```, and run it:

```
java -jar ${app.jar}
```

Launch application, change ${app.jar} to jar's package name.

### Verify

#### Zuul Invoke

```shell
curl -L -X GET 'http://localhost:48082/GatewayCalleeService/gateway/example/callee/echo' -H 'SCT-CUSTOM-METADATA: {"b": 2}'
```

Expected return rate

```
{"a":"1","b":2}
```

#### Spring-Cloud-Gateway Invoke

```shell
curl -L -X GET 'http://localhost:48083/GatewayCalleeService/gateway/example/callee/echo' -H 'SCT-CUSTOM-METADATA: {"b": 2}'
```

Expected return rate

```
{"a":"1","b":2}
```

#### Gateway Rate Limit

See [Polaris RateLimit Example](../polaris-ratelimit-example/README.md)

