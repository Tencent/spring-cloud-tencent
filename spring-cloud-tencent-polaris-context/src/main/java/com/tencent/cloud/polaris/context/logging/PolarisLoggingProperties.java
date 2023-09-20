package com.tencent.cloud.polaris.context.logging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * The properties for polaris logging.
 *
 * @author wenxuan70
 */
@ConfigurationProperties(prefix = "spring.cloud.polaris.logging")
public class PolarisLoggingProperties {

	/**
	 * logging path.
	 */
	@Value("${spring.cloud.polaris.logging.path:#{systemProperties['user.dir'] + T(java.io.File).separator + 'polaris' + T(java.io.File).separator + 'logs'}}")
	private String path;

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		System.out.println("设置spring.cloud.polaris.logging.path: " + path);
		this.path = path;
	}

	@Override
	public String toString() {
		return "PolarisLoggingProperties{" +
				"path='" + path + '\'' +
				'}';
	}
}
