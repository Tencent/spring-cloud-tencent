# Spring Cloud Polaris Circuitbreaker example

## Example Introduction

This example shows how to use```spring-cloud-starter-tencent-polaris-circuitbreaker```in Spring Cloud project for its features.

This example contains callee-service```polaris-circuitbreaker-callee-service```、```polaris-circuitbreaker-callee-service2```and caller-service```polaris-circuitbreaker-feign-example```、```polaris-circuitbreaker-gateway-example```、```polaris-circuitbreaker-webclient-example```.

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

#### Launching callee service

Launching```polaris-circuitbreaker-example/polaris-circuitbreaker-callee-service```、```polaris-circuitbreaker-example/polaris-circuitbreaker-callee-service2```


#### Launching caller service

##### Launching Feign and Verify

Launching```polaris-circuitbreaker-example/polaris-circuitbreaker-feign-example```.

Sending request`curl --location --request GET 'http://127.0.0.1:48080/example/service/a/getBServiceInfo/fallbackFromPolaris'`, Verify circuit breaker and fallback from Polaris-server.

Sending request`curl --location --request GET 'http://127.0.0.1:48080/example/service/a/getBServiceInfo/fallbackFromCode'`, Verify circuit breaker and fallback from code.

##### Launching RestTemplate and Verify

Launching```polaris-circuitbreaker-example/polaris-circuitbreaker-resttemplate-example```.

Sending request`curl --location --request GET 'http://127.0.0.1:48080/example/service/a/getBServiceInfo/fallbackFromPolaris'`, Verify circuit breaker and fallback from Polaris-server.

Sending request`curl --location --request GET 'http://127.0.0.1:48080/example/service/a/getBServiceInfo/fallbackFromCode'`, Verify circuit breaker and fallback from code.

##### Launching WebClient and Verify

Launching```polaris-circuitbreaker-example/polaris-circuitbreaker-webclient-example```。

Sending request`curl --location --request GET 'http://127.0.0.1:48080/example/service/a/getBServiceInfo'`, Verify circuit breaker and fallback from code.

##### Launching SCG and Verify

Launching```polaris-circuitbreaker-example/polaris-circuitbreaker-gateway-example```。

Sending request`curl --location --request GET 'http://127.0.0.1:48080/polaris-circuitbreaker-callee-service/example/service/b/info'`, Verify circuit breaker and fallback from code.

Changing```polaris-circuitbreaker-example/polaris-circuitbreaker-gateway-example/resources/bootstrap.yml```, delete local fallback and restart, Verify circuit breaker and fallback from Polaris-server.

