# Spring Cloud Tencent Lossless Example

## Offline Callback

The lossless callee declares a `LosslessOfflineCallback` Bean. Before the Polaris instance is deregistered,
SCT executes callbacks in Spring order. The example callback only logs the instance identity, so it can run
without SOFA Registry or a message queue.

```java
@Bean
@Order(100)
public LosslessOfflineCallback offlineRecordCallback() {
    return context -> LOGGER.info("Instance {}:{} is going offline.",
            context.getHost(), context.getPort());
}
```

The relevant configuration is:

```yaml
spring:
  cloud:
    polaris:
      lossless:
        enabled: true
        offline-callback-enabled: true
        offline-callback-timeout: 10000
        offline-callback-total-timeout: 20000
        offline-callback-failure-mode: continue
```

Call `/offline` with an explicit client timeout. With the default callback budget and the Polaris request
budget, 25 seconds leaves scheduling and response margin:

```shell
curl --max-time 25 http://127.0.0.1:28084/offline
```

HTTP 200 means the Polaris action completed successfully. In `continue` mode it does not mean that every
business callback succeeded: callback failures, timeouts, and skipped callbacks are visible in the logs.

Set `offline-callback-failure-mode` to `abort` when a callback failure must stop the action before Polaris
deregistration. Business side effects are not rolled back automatically.

Repeated offline requests for the same registration lifecycle reuse the completed result and do not run the
callbacks again. A later re-registration creates a new lifecycle with a fresh callback chain.

In `abort` mode, a callback failure is retained for the current lifecycle. Repeating `/offline` returns the
same failure without replaying callbacks, including callbacks that completed before the failure. Re-register
the instance before retrying the callback chain.
