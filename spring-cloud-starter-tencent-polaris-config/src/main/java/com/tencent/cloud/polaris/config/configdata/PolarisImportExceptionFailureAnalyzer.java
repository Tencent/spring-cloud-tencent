package com.tencent.cloud.polaris.config.configdata;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.cloud.commons.ConfigDataMissingEnvironmentPostProcessor;

/**
 * Class for most {@code FailureAnalyzer} implementations, to analyze ImportException when
 * miss Polaris configData config.
 * <p>Refer to the Nacos project implementation：
 * <code><a href=https://github.com/alibaba/spring-cloud-alibaba/blob/2021.x/spring-cloud-alibaba-starters/spring-cloud-starter-alibaba-nacos-config/src/main/java/com/alibaba/cloud/nacos/configdata/NacosConfigDataMissingEnvironmentPostProcessor.java>
 * ImportExceptionFailureAnalyzer</a></code>
 *
 * @author wlx
 * @see AbstractFailureAnalyzer
 */
public class PolarisImportExceptionFailureAnalyzer extends
		AbstractFailureAnalyzer<ConfigDataMissingEnvironmentPostProcessor.ImportException> {

	@Override
	protected FailureAnalysis analyze(Throwable rootFailure, ConfigDataMissingEnvironmentPostProcessor.ImportException cause) {
		String description;
		if (cause.missingPrefix) {
			description = "The spring.config.import property is missing a " + PolarisConfigDataLocationResolver.PREFIX
					+ " entry";
		}
		else {
			description = "No spring.config.import property has been defined";
		}
		String action = "Add a spring.config.import=polaris property to your configuration.\n"
				+ "\tIf configuration is not required add spring.config.import=optional:polaris instead.\n"
				+ "\tTo disable this check, set spring.cloud.polaris.config.import-check.enabled=false.";
		return new FailureAnalysis(description, action, cause);
	}
}
