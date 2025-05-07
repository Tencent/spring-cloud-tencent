package org.springframework.tsf.core.util;

import com.tencent.cloud.common.util.ApplicationContextAwareUtils;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

/**
 * Spring context utils.
 * <p>
 * Deprecated since 2.0.0.0.
 *
 * @author hongweizhu
 */
@Deprecated
public class TsfSpringContextAware {

	/**
	 * Get application context.
	 * @return application context
	 */
	public static ApplicationContext getApplicationContext() {
		return ApplicationContextAwareUtils.getApplicationContext();
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		// do nothing.
	}

	/**
	 * Get application property.
	 * @param key property name
	 * @return property value
	 */
	public static String getProperties(String key) {
		return ApplicationContextAwareUtils.getProperties(key);
	}

	/**
	 * Get application property. If null, return default.
	 * @param key property name
	 * @param defaultValue default value
	 * @return property value
	 */
	public static String getProperties(String key, String defaultValue) {
		return ApplicationContextAwareUtils.getProperties(key, defaultValue);
	}

	public static <T> T getBean(Class<T> requiredType) {
		return ApplicationContextAwareUtils.getBean(requiredType);
	}
}
