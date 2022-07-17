package com.tencent.cloud.polaris.config.configdata;

import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.cloud.commons.ConfigDataMissingEnvironmentPostProcessor;
import org.springframework.cloud.util.PropertyUtils;
import org.springframework.core.env.Environment;

/**
 * PolarisConfigDataMissingEnvironmentPostProcessor to check if miss PolarisConfigData config,if miss config
 * will throw {@link ImportException}.
 *
 * @author wlx
 * @see ConfigDataMissingEnvironmentPostProcessor
 * @see ConfigDataMissingEnvironmentPostProcessor.ImportException
 */
public class PolarisConfigDataMissingEnvironmentPostProcessor extends ConfigDataMissingEnvironmentPostProcessor {

	/**
	 * run after {@link ConfigDataEnvironmentPostProcessor}.
	 */
	public static final int ORDER = ConfigDataEnvironmentPostProcessor.ORDER + 1;

	@Override
	public int getOrder() {
		return ORDER;
	}

	@Override
	protected boolean shouldProcessEnvironment(Environment environment) {
		// if using bootstrap or legacy processing don't run
		if (!PropertyUtils.bootstrapEnabled(environment) && !PropertyUtils.useLegacyProcessing(environment)) {
			boolean configEnabled = environment.getProperty("spring.cloud.polaris.config.enabled", Boolean.class, true);
			boolean importCheckEnabled = environment.getProperty("spring.cloud.polaris.config.import-check.enabled", Boolean.class, true);
			return configEnabled && importCheckEnabled;
		}
		else {
			return false;
		}
	}

	@Override
	protected String getPrefix() {
		return PolarisConfigDataLocationResolver.PREFIX;
	}
}
