# Spring Cloud Tencent Metadata

## Usage

### Transitive Custom Metadata

> Transitive custom metadata can be transferred from caller to callee.

- Get all transitive custom metadata with read-only.
```
Map<String, String> customMetadataMap = MetadataContextHolder.get().getAllTransitiveCustomMetadata();
```

- Get transitive custom metadata with key.
```
String value = MetadataContextHolder.get().getTransitiveCustomMetadata(KEY);
```

- Put transitive custom metadata with key-value.
```
MetadataContextHolder.get().putTransitiveCustomMetadata(KEY, VALUE);
```

- Put transitive custom metadata with another map.
```
MetadataContextHolder.get().putAllTransitiveCustomMetadata(ANOTHER_MAP);
```

### System Metadata

> System metadata cannot be transferred.

- Get all system metadata with read-only.
```
Map<String, String> systemMetadataMap = MetadataContextHolder.get().getAllSystemMetadata();
```

- Get system metadata with key.
```
String value = MetadataContextHolder.get().getSystemMetadata(KEY);
```

- Put system metadata with key-value.
```
MetadataContextHolder.get().putSystemMetadata(KEY, VALUE);
```

- Put system metadata with another map.
```
MetadataContextHolder.get().putAllSystemMetadata(ANOTHER_MAP);
```

- Map key list:
- LOCAL_NAMESPACE
- LOCAL_SERVICE
- LOCAL_PATH
- PEER_NAMESPACE
- PEER_SERVICE
- PEER_PATH
