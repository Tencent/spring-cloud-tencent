package com.tencent.cloud.rpc.enhancement.stat.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The properties for stat reporter pushgateway.
 *
 * @author Haotian Zhang
 */
@ConfigurationProperties("spring.cloud.polaris.stat.pushgateway")
public class PolarisStatPushGatewayProperties {
	/**
	 * If state pushGateway reporter enabled.
	 */
	private boolean enabled = false;

	/**
	 * PushGateway namespace.
	 */
	private String namespace = "Polaris";

	/**
	 * PushGateway service.
	 */
	private String service = "polaris.pushgateway";

	/**
	 * Push metrics interval.
	 * unit: milliseconds default 60s.
	 */
	private Long pushInterval = 60 * 1000L;

	/**
	 * If push gateway gzip open. default false.
	 */
	private Boolean openGzip = false;

	private List<String> address;

	boolean isEnabled() {
		return enabled;
	}

	void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	String getNamespace() {
		return namespace;
	}

	void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	String getService() {
		return service;
	}

	void setService(String service) {
		this.service = service;
	}

	Long getPushInterval() {
		return pushInterval;
	}

	void setPushInterval(Long pushInterval) {
		this.pushInterval = pushInterval;
	}

	Boolean getOpenGzip() {
		return openGzip;
	}

	void setOpenGzip(Boolean openGzip) {
		this.openGzip = openGzip;
	}

	List<String> getAddress() {
		return address;
	}

	void setAddress(List<String> address) {
		this.address = address;
	}

	@Override
	public String toString() {
		return "PolarisStatPushGatewayProperties{" +
				"enabled=" + enabled +
				", namespace='" + namespace + '\'' +
				", service='" + service + '\'' +
				", pushInterval=" + pushInterval +
				", openGzip=" + openGzip +
				", address=" + address +
				'}';
	}
}
