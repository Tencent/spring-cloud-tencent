# Spring Cloud Tencent Metadata Transfer example

## Example Introduction

This example shows how to use ```spring-cloud-starter-tencent-metadata-transfer``` in Spring Cloud project for its
features.

This example contains ```metadata-callee-service```、```metadata-caller-service```.

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

### Maven Dependency

```xml
<dependency>
    <groupId>com.tencent.cloud</groupId>
    <artifactId>spring-cloud-starter-tencent-metadata-transfer</artifactId>
</dependency>
```

### Launching Example

#### Launching Polaris Backend Service

Reference to [Polaris Getting Started](https://github.com/PolarisMesh/polaris#getting-started)

#### Launching Application

- IDEA Launching

- ```spring-cloud-tencent-examples/metadata-transfer-example/metadata-callee-service```‘s```MetadataCalleeService```
- ```spring-cloud-tencent-examples/metadata-transfer-example/metadata-caller-service```'s```MetadataCallerService```

- Maven Package Launching

Execute under ```spring-cloud-tencent-examples/metadata-transfer-example```

```sh
mvn clean package
```

Then find the jars under ```metadata-callee-service```、```metadata-caller-service```, and run it:

```
java -jar ${app.jar}
```

Launch application, change ${app.jar} to jar's package name.


### Metadata Configuration

In the ```bootstrap.yml``` configuration file of the ```spring-cloud-tencent-examples/metadata-transfer-example/metadata-caller-service``` project

```yaml
spring:
  cloud:
    tencent:
      metadata:
        # Defined your metadata keys & values
        content:
          # Example: intransitive
          CUSTOM-METADATA-KEY-LOCAL: CUSTOM-VALUE-LOCAL
          # Example: transitive
          CUSTOM-METADATA-KEY-TRANSITIVE: CUSTOM-VALUE-TRANSITIVE
        # Assigned which metadata key-value will be passed along the link
        transitive:
          - CUSTOM-METADATA-KEY-TRANSITIVE

```

### Verify

#### Request Invoke

```shell
curl -L -X GET 'http://127.0.0.1:48080/metadata/service/caller/feign/info'
```

Expected return rate

```
{
  "caller-metadata-contents": {
    "CUSTOM-METADATA-KEY-TRANSITIVE": "CUSTOM-VALUE-TRANSITIVE",
    "CUSTOM-METADATA-KEY-LOCAL": "CUSTOM-VALUE-LOCAL"
  },
  "callee-transitive-metadata": {
    "CUSTOM-METADATA-KEY-TRANSITIVE": "CUSTOM-VALUE-TRANSITIVE"
  },
  "caller-transitive-metadata": {
    "CUSTOM-METADATA-KEY-TRANSITIVE": "CUSTOM-VALUE-TRANSITIVE"
  }
}
```

Response value description

- Key `caller-metadata-contents` represents all metadata configured by default in the `metadata-caller-service` project.
- Key `caller-transitive-metadata` represents the list of metadata that can be passed in the link specified in the `metadata-caller-service` item.
- Key `callee-transitive-metadata` represents the list of upstream metadata passed when the `metadata-callee-service` project is called by `metadata-caller-service`.

### Wiki Reference

See [Spring Cloud Tencent Metadata Transfer Usage Document](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Metadata-Transfer-Usage-Document) for more reference .
