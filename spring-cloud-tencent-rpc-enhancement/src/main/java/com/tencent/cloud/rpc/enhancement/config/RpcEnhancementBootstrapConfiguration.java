package com.tencent.cloud.rpc.enhancement.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Bootstrap configuration for rpc enhancement.
 * @author lepdou 2022-08-24
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty("spring.cloud.polaris.enabled")
@Import(RpcEnhancementAutoConfiguration.class)
public class RpcEnhancementBootstrapConfiguration {
}
