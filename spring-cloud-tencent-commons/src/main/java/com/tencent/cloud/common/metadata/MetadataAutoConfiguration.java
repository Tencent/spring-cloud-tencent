package com.tencent.cloud.common.metadata;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadata auto configuration.
 *
 * @author Haotian Zhang
 */
@Configuration
public class MetadataAutoConfiguration {

	/**
	 * metadata properties.
	 * @return metadata properties
	 */
	@Bean
	public MetadataLocalProperties metadataLocalProperties() {
		return new MetadataLocalProperties();
	}

}
