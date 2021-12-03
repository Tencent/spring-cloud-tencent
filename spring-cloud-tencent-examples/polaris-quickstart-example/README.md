# Spring Cloud Polaris Quickstart example

## Example Introduction

This example shows how to make application integrated with spring-cloud-tencent rapidly.

## Instruction

### Configuration

Modify bootstrap.yml, ${ip} and ${port} is the address of polaris server.

```yaml
spring:
  application:
    name: EchoService
  cloud:
    polaris:
      address: grpc://${ip}:${port}
```

### Launching Example

#### Launching Application

- Start in IDEA

Find main class EchoServiceApplication in project polaris-quickstart-example, and execute the main method.

- Start by fatjar

Run build command in ```spring-cloud-tencent-examples/polaris-quickstart-example```:

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

```shell
curl -L -X GET 'http://localhost:47080/quickstart/feign?msg=hello_world''
```

expect：hello_world