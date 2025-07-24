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
	@Value("${spring.cloud.polaris.stat.pushgateway.enabled:#{false}}")
	private boolean pushGatewayEnabled = false;

	/**
	 * PushGateway namespace.
	 */
	@Value("${spring.cloud.polaris.stat.pushgateway.namespace:Polaris}")
	private String statNamespace = "Polaris";

	/**
	 * PushGateway service.
	 */
	@Value("${spring.cloud.polaris.stat.pushgateway.service:polaris.pushgateway}")
	private String statService = "polaris.pushgateway";

	/**
	 * Push metrics interval.
	 * unit: milliseconds default 60s.
	 */
	@Value("${spring.cloud.polaris.stat.pushgateway.push-interval:#{60000}}")
	private Long pushGatewayPushInterval = 60 * 1000L;

	/**
	 * If push gateway gzip open. default false.
	 */
	@Value("${spring.cloud.polaris.stat.pushgateway.open-gzip:#{false}}")
	private Boolean openGzip = false;

	private List<String> address;

	boolean isPushGatewayEnabled() {
		return pushGatewayEnabled;
	}

	void setPushGatewayEnabled(boolean pushGatewayEnabled) {
		this.pushGatewayEnabled = pushGatewayEnabled;
	}

	String getStatNamespace() {
		return statNamespace;
	}

	void setStatNamespace(String statNamespace) {
		this.statNamespace = statNamespace;
	}

	String getStatService() {
		return statService;
	}

	void setStatService(String statService) {
		this.statService = statService;
	}

	Long getPushGatewayPushInterval() {
		return pushGatewayPushInterval;
	}

	void setPushGatewayPushInterval(Long pushGatewayPushInterval) {
		this.pushGatewayPushInterval = pushGatewayPushInterval;
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
}
