package com.tencent.cloud.polaris.context.logging;

import com.tencent.cloud.polaris.context.ConditionalOnPolarisEnabled;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguration for polaris logging.
 *
 * @author wenxuan70
 */
@ConditionalOnPolarisEnabled
@EnableConfigurationProperties({PolarisLoggingProperties.class})
public class PolarisLoggingAutoConfiguration {

	@Bean
	public PolarisLoggingProperties polarisLoggingProperties() {
		return new PolarisLoggingProperties();
	}
}
