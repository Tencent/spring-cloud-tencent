package com.tencent.cloud.polaris.context.logging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;

/**
 * Bootstrap autoconfiguration for polaris logging.
 *
 * @author wenxuan70
 */
@ConditionalOnProperty("spring.cloud.polaris.enabled")
@Import(PolarisLoggingAutoConfiguration.class)
public class PolarisLoggingBootstrapAutoConfiguration {
}
