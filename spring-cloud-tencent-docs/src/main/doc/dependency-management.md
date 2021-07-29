# dependencyManagement

if you want to use Spring Cloud Tencent micro-service software development kit, you can depend on the bom as below, add code as below at ```<dependencyManagement>``` in pom.xml. Going forward, continue using dependencies in bom no longer needs version number.

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.tencent.cloud</groupId>
            <artifactId>spring-cloud-tencent-dependencies</artifactId>
            <version>0.1.0.Hoxton.BETA</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

