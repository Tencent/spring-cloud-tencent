package com.tencent.cloud.polaris.registry.graceful;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("spring.cloud.service-registry.graceful-registration")
public class GracefulServiceRegistrationProperties {

	/** Whether service graceful-registration is enabled. Defaults to true. */
	private boolean enabled = true;

	/** Whether to register the management as a service. Defaults to true. */
	private boolean registerManagement = true;

	/**
	 * Whether startup fails if there is no AutoServiceRegistration. Defaults to false.
	 */
	private boolean failFast = false;

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public boolean isRegisterManagement() {
		return this.registerManagement;
	}

	public void setRegisterManagement(boolean registerManagement) {
		this.registerManagement = registerManagement;
	}

	@Deprecated
	public boolean shouldRegisterManagement() {
		return this.registerManagement;
	}

	public boolean isFailFast() {
		return this.failFast;
	}

	public void setFailFast(boolean failFast) {
		this.failFast = failFast;
	}

}
