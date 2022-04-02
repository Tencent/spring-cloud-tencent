# Spring Cloud Polaris CircuitBreaker Example

## Example Introduction

This example shows how to use```spring-cloud-starter-tencent-polaris-circuitbreaker``` in Spring Cloud project and other features

This example is divided to two microservice, ```polaris-circuitbreaker-example-a``` and ```polaris-circuitbreaker-example-b```. In these two microservices, ```polaris-circuitbreaker-example-a``` invokes ```polaris-circuitbreaker-example-b```.

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

###Launching Example

###Launching Polaris Backend Service

Reference to [Polaris Getting Started](https://github.com/PolarisMesh/polaris#getting-started)

####Launching Application

Note, because verification is needed for circuit-break feature, therefore, one needs to deploy more than two invoked services (two deployment in this example)

Launching```spring-cloud-tencent-examples/polaris-circuitbreaker-example/polaris-circuitbreaker-example-a```'s ServiceA and ```spring-cloud-tencent-examples/polaris-circuitbreaker-example/polaris-circuitbreaker-example-b```'s ServiceB

note, Service B needs to launch two. One can adjust the port on the same machine.

Two Services B's ```com.tencent.cloud.polaris.circuitbreaker.example.ServiceBController.info``` logics are different. One returns normally, one is abnormal.

- Maven Package Launching

Execute under ```spring-cloud-tencent-examples/polaris-discovery-example```

note, Service B needs to launch two. One can adjust the port on the same machine.

Two Services B's com.tencent.cloud.polaris.circuitbreaker.example.ServiceBController.info logics are different. One returns normally, one is abnormal.

```sh
mvn clean package
```

Then under ``polaris-circuitbreaker-example-a``` and ``polaris-circuitbreaker-example-b``` find the package that generated jar, and run it

```
java -jar ${app.jar}
```

Launch application, change ${app.jar} to jar's package name

##Verify

####Feign Invoke

Execute the following orders to invoke Feign, the logic is ```ServiceB``` has an abnormal signal

```shell
curl -L -X GET 'localhost:48080/example/service/a/getBServiceInfo'
```

Expected return condition:

when appear

```
trigger the refuse for service b
```

it means the request signals abnormal ServiceB, and will ciruitbreak this instance, the later requests will return normally. 
