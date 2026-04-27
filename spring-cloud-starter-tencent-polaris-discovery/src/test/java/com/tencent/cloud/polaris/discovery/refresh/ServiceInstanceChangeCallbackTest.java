/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.tencent.cloud.polaris.discovery.refresh;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.tencent.cloud.polaris.registry.PolarisAutoServiceRegistration;
import com.tencent.polaris.api.pojo.Instance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Test for {@link ServiceInstanceChangeCallback}.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT,
		classes = ServiceInstanceChangeCallbackTest.TestApplication.class,
		properties = {"spring.config.location = classpath:application-test.yml",
				"spring.main.web-application-type = servlet",
				"spring.cloud.gateway.server.webflux.enabled = false"})
public class ServiceInstanceChangeCallbackTest {

	@Autowired
	ServiceInstanceChangeCallbackManager serviceInstanceChangeCallbackManager;

	@Test
	public void test1() {
		// Get callbackMap from serviceInstanceChangeCallbackManager via reflection
		try {
			Field callbackMapField = ServiceInstanceChangeCallbackManager.class.getDeclaredField("callbackMap");
			callbackMapField.setAccessible(true);
			ConcurrentHashMap<String, List<ServiceInstanceChangeCallback>> callbackMap =
					(ConcurrentHashMap<String, List<ServiceInstanceChangeCallback>>) callbackMapField.get(serviceInstanceChangeCallbackManager);

			// Verify
			assertThat(callbackMap.containsKey("java_provider_test")).isTrue();
			assertThat(callbackMap.containsKey("QuickstartCalleeService")).isTrue();
			// ignore error and empty
			assertThat(callbackMap.size()).isEqualTo(2);

		}
		catch (Exception e) {
			throw new RuntimeException("Failed to get callbackMap via reflection", e);
		}
	}

	@SpringBootApplication
	protected static class TestApplication {

		@Bean
		public SelfServiceChangeCallback selfServiceChangeCallback() {
			return new SelfServiceChangeCallback();
		}

		@Bean
		public CalleeServiceChangeCallback calleeServiceChangeCallback() {
			return new CalleeServiceChangeCallback();
		}

		@Bean
		public ErrorServiceChangeCallback errorServiceChangeCallback() {
			return new ErrorServiceChangeCallback();
		}

		@Bean
		public EmptyServiceChangeCallback emptyServiceChangeCallback() {
			return new EmptyServiceChangeCallback();
		}

		@Bean
		public ParsingEmptyServiceChangeCallback parseEmptyServiceChangeCallback() {
			return new ParsingEmptyServiceChangeCallback();
		}

		@Bean
		public TestBeanPostProcessor testBeanPostProcessor() {
			return new TestBeanPostProcessor();
		}

	}

	@ServiceInstanceChangeListener(serviceName = "${spring.application.name}")
	static class SelfServiceChangeCallback implements ServiceInstanceChangeCallback {

		@Override
		public void callback(List<Instance> currentServiceInstances, List<Instance> addServiceInstances, List<Instance> deleteServiceInstances) {

		}
	}

	@ServiceInstanceChangeListener(serviceName = "${error.name}")
	static class ErrorServiceChangeCallback implements ServiceInstanceChangeCallback {

		@Override
		public void callback(List<Instance> currentServiceInstances, List<Instance> addServiceInstances, List<Instance> deleteServiceInstances) {

		}
	}

	@ServiceInstanceChangeListener(serviceName = "${test.empty}")
	static class ParsingEmptyServiceChangeCallback implements ServiceInstanceChangeCallback {

		@Override
		public void callback(List<Instance> currentServiceInstances, List<Instance> addServiceInstances, List<Instance> deleteServiceInstances) {

		}
	}

	@ServiceInstanceChangeListener(serviceName = "")
	static class EmptyServiceChangeCallback implements ServiceInstanceChangeCallback {

		@Override
		public void callback(List<Instance> currentServiceInstances, List<Instance> addServiceInstances, List<Instance> deleteServiceInstances) {

		}
	}

	@ServiceInstanceChangeListener(serviceName = "QuickstartCalleeService")
	static class CalleeServiceChangeCallback implements ServiceInstanceChangeCallback {

		@Override
		public void callback(List<Instance> currentServiceInstances, List<Instance> addServiceInstances, List<Instance> deleteServiceInstances) {

		}
	}

	static class TestBeanPostProcessor implements BeanPostProcessor {
		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof PolarisAutoServiceRegistration) {
				return org.mockito.Mockito.mock(PolarisAutoServiceRegistration.class);
			}
			return bean;
		}
	}
}
