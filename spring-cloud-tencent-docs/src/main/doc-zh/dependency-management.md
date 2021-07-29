# 依赖管理

如果您想使用Spring Cloud Tencent微服务开发套件，您可以直接依赖以下bom，即在pom.xml的<dependencyManagement>中添加如下代码。后续使用bom下的依赖无需带版本号即可引入。

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