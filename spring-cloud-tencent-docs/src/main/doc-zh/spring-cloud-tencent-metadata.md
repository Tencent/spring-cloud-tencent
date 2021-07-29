# Spring Cloud Tencent Metadata

## 使用方式

### 可传递的自定义metadata

> 可以从主调方传递给被调方的metadata

- 获取只读的所有可传递的自定义metadata的映射表
```
Map<String, String> customMetadataMap = MetadataContextHolder.get().getAllTransitiveCustomMetadata();
```

- 根据key获取可传递的自定义metadata
```
String value = MetadataContextHolder.get().getTransitiveCustomMetadata(KEY);
```

- 以key-value形式保存可传递的自定义metadata
```
MetadataContextHolder.get().putTransitiveCustomMetadata(KEY, VALUE);
```

- 从一个映射表中读取并保存到可传递的自定义metadata映射表中
```
MetadataContextHolder.get().putAllTransitiveCustomMetadata(ANOTHER_MAP);
```

### 系统metadata

> 系统metadata不可被传递。

- 获取只读的所有系统metadata的映射表
```
Map<String, String> systemMetadataMap = MetadataContextHolder.get().getAllSystemMetadata();
```

- 根据key获取系统metadata
```
String value = MetadataContextHolder.get().getSystemMetadata(KEY);
```

- 以key-value形式保存系统metadata
```
MetadataContextHolder.get().putSystemMetadata(KEY, VALUE);
```

- 从一个映射表中读取并保存到系统metadata映射表中
```
MetadataContextHolder.get().putAllSystemMetadata(ANOTHER_MAP);
```

- 系统metadata映射表的key如下所示：
- LOCAL_NAMESPACE
- LOCAL_SERVICE
- LOCAL_PATH
- PEER_NAMESPACE
- PEER_SERVICE
- PEER_PATH
