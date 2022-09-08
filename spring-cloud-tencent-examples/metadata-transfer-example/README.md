# Spring Cloud Tencent Metadata Transfer example

## Example Introduction

This example shows how to use ```spring-cloud-starter-tencent-metadata-transfer``` in Spring Cloud project for its
features.

This example contains ```metadata-frontend```、```metadata-middle```、```metadata-backend```.

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

- ```spring-cloud-tencent-examples/metadata-transfer-example/metadata-frontend```'s ```MetadataFrontendService```
- ```spring-cloud-tencent-examples/metadata-transfer-example/metadata-middle```'s ```MetadataMiddleService```
- ```spring-cloud-tencent-examples/metadata-transfer-example/metadata-backend```'s ```MetadataBackendService```


- Maven Package Launching

Execute under ```spring-cloud-tencent-examples/metadata-transfer-example```

```sh
mvn clean package
```

Then find the jars under ```metadata-frontend```、```metadata-middle```、```metadata-backend```, and run it:

```
java -jar ${app.jar}
```

Launch application, change ${app.jar} to jar's package name.

### Metadata Configuration

- In the ```bootstrap.yml``` configuration file of
  the ```spring-cloud-tencent-examples/metadata-transfer-example/metadata-frontend``` project

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
          CUSTOM-METADATA-KEY-TRANSITIVE: CUSTOM-VALUE-TRANSITIVE-FRONTEND
          # Example: disposable
          CUSTOM-METADATA-KEY-DISPOSABLE: CUSTOM-VALUE-DISPOSABLE-FRONTEND
        # Assigned which metadata key-value will be passed along the link
        transitive:
          - CUSTOM-METADATA-KEY-TRANSITIVE
        # Specify which metadata key value will be passed only once (one-step)
        disposable:
          - CUSTOM-METADATA-KEY-DISPOSABLE

```

- In the ```bootstrap.yml``` configuration file of
  the ```spring-cloud-tencent-examples/metadata-transfer-example/metadata-middle``` project

```yaml
spring:
  cloud:
    tencent:
      metadata:
        # Defined your metadata keys & values
        content:
          # Example: intransitive
          CUSTOM-METADATA-KEY-LOCAL-2: CUSTOM-VALUE-LOCAL-2
          # Example: transitive
          CUSTOM-METADATA-KEY-TRANSITIVE-2: CUSTOM-VALUE-TRANSITIVE-2
          # Example: disposable
          CUSTOM-METADATA-KEY-DISPOSABLE: CUSTOM-VALUE-DISPOSABLE-MIDDLE
        # Assigned which metadata key-value will be passed along the link
        transitive:
          - CUSTOM-METADATA-KEY-TRANSITIVE-2
        # Specify which metadata key value will be passed only once (one-step)
        disposable:
          - CUSTOM-METADATA-KEY-DISPOSABLE
```

### Verify

#### Request Invoke

```shell
curl -L -X GET 'http://127.0.0.1:48080/metadata/service/caller/feign/info'
```

Expected return rate

```
{
  "frontend-transitive-metadata": {
    "CUSTOM-METADATA-KEY-TRANSITIVE": "CUSTOM-VALUE-TRANSITIVE-FRONTEND"
  },
  "frontend-upstream-disposable-metadata": {
  },
  "frontend-local-disposable-metadata": {
    "CUSTOM-METADATA-KEY-DISPOSABLE": "CUSTOM-VALUE-DISPOSABLE-FRONTEND"
  },
  
  "middle-transitive-metadata": {
    "CUSTOM-METADATA-KEY-TRANSITIVE": "CUSTOM-VALUE-TRANSITIVE-FRONTEND",
    "CUSTOM-METADATA-KEY-TRANSITIVE-2": "CUSTOM-VALUE-TRANSITIVE-2"
  },
  "middle-upstream-disposable-metadata": {
    "CUSTOM-METADATA-KEY-DISPOSABLE": "CUSTOM-VALUE-DISPOSABLE-FRONTEND"
  },
  "middle-local-disposable-metadata": {
    "CUSTOM-METADATA-KEY-DISPOSABLE": "CUSTOM-VALUE-DISPOSABLE-MIDDLE"
  },
  
  "backend-transitive-metadata": {
    "CUSTOM-METADATA-KEY-TRANSITIVE": "CUSTOM-VALUE-TRANSITIVE-FRONTEND",
    "CUSTOM-METADATA-KEY-TRANSITIVE-2": "CUSTOM-VALUE-TRANSITIVE-2"
  },
  "backend-upstream-disposable-metadata": {
    "CUSTOM-METADATA-KEY-DISPOSABLE": "CUSTOM-VALUE-DISPOSABLE-MIDDLE"
  },
  "backend-local-disposable-metadata": {
  }
}
```

Response value description

> `*` (asterisk), representing `frontend`, `middle`, `backend` in the example.

- Key `*-transitive-metadata` represents all the passable (fully linked) metadata configured by default in the service.
- Key `*-upstream-disposable-metadata` indicates the one-time transmissible metadata obtained from upstream requests in
  the service.
- Key `*-local-disposable-metadata` indicates the one-time metadata passed downstream as configured by the current
  service.

### How to get the passed metadata via Api

- Get the metadata passed globally

```
MetadataContext context=MetadataContextHolder.get();
Map<String, String> customMetadataMap=context.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);

customMetadataMap.forEach((key,value)->{
    // ...
});
```

- Get disposable(one-time) metadata passed from upstream

```
Map<String, String> upstreamDisposableMetadatas=MetadataContextHolder.getAllDisposableMetadata(true);
upstreamDisposableMetadatas.forEach((key,value)->{
	// ...
});
```

- Get disposable(one-time) metadata for local configuration

```
Map<String, String> localDisposableMetadatas=MetadataContextHolder.getAllDisposableMetadata(false);
localDisposableMetadatas.forEach((key,value)->{
	// ...
});
```

### Wiki Reference

See [Spring Cloud Tencent Metadata Transfer Usage Document](https://github.com/Tencent/spring-cloud-tencent/wiki/Spring-Cloud-Tencent-Metadata-Transfer-Usage-Document)
for more reference.
