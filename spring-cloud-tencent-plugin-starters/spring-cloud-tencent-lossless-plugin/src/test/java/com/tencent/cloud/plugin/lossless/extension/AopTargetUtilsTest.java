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

package com.tencent.cloud.plugin.lossless.extension;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.target.HotSwappableTargetSource;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.cloud.client.serviceregistry.ServiceRegistry;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for {@link AopTargetUtils}.
 *
 * @author evelynwei
 */
@DisplayName("AopTargetUtils")
class AopTargetUtilsTest {

	/**
	 * Verify candidate resolution does not borrow an object from a dynamic target source.
	 */
	@DisplayName("test dynamic target source is not borrowed")
	@Test
	void testDynamicTargetSourceIsNotBorrowed() {
		for (boolean opaque : new boolean[] { false, true }) {
			// Arrange
			CountingTargetSource targetSource = new CountingTargetSource(new TargetRegistry());
			ProxyFactory proxyFactory = new ProxyFactory();
			proxyFactory.setTargetSource(targetSource);
			proxyFactory.setInterfaces(ServiceRegistry.class);
			proxyFactory.setProxyTargetClass(false);
			proxyFactory.setOpaque(opaque);
			@SuppressWarnings("unchecked")
			ServiceRegistry<Registration> proxy = (ServiceRegistry<Registration>) proxyFactory.getProxy();

			// Act
			AopTargetUtils.TargetInfo targetInfo = AopTargetUtils.resolveTarget(proxy);

			// Assert
			assertThat(targetInfo.getTarget()).as("opaque=" + opaque).isNull();
			assertThat(targetInfo.getTargetClass()).as("opaque=" + opaque).isEqualTo(TargetRegistry.class);
			assertThat(targetInfo.isDynamicTarget()).as("opaque=" + opaque).isTrue();
			assertThat(targetSource.getActiveCount()).as("opaque=" + opaque).isZero();
			assertThat(targetSource.getGetCount()).as("opaque=" + opaque).isZero();
			assertThat(targetSource.getReleaseCount()).as("opaque=" + opaque).isZero();

			proxy.deregister(new TargetRegistration());
			assertThat(targetSource.getActiveCount()).as("opaque=" + opaque).isZero();
			assertThat(targetSource.getGetCount()).as("opaque=" + opaque).isEqualTo(1);
			assertThat(targetSource.getReleaseCount()).as("opaque=" + opaque).isEqualTo(1);
		}
	}

	/**
	 * Verify opaque CGLIB and JDK proxies resolve the actual target and keep HotSwap state.
	 */
	@DisplayName("test opaque proxy target resolution")
	@Test
	void testOpaqueProxyTargetResolution() {
		for (String proxyType : new String[] { "cglib", "jdk-hot-swap" }) {
			// Arrange
			TargetRegistry target = new TargetRegistry();
			ProxyFactory proxyFactory = new ProxyFactory();
			if ("cglib".equals(proxyType)) {
				proxyFactory.setTarget(target);
				proxyFactory.setProxyTargetClass(true);
			}
			else {
				proxyFactory.setTargetSource(new HotSwappableTargetSource(target));
				proxyFactory.setInterfaces(ServiceRegistry.class);
				proxyFactory.setProxyTargetClass(false);
			}
			proxyFactory.setOpaque(true);
			@SuppressWarnings("unchecked")
			ServiceRegistry<Registration> proxy = (ServiceRegistry<Registration>) proxyFactory.getProxy();

			// Act
			AopTargetUtils.TargetInfo targetInfo = AopTargetUtils.resolveTarget(proxy);

			// Assert
			assertThat(targetInfo.getTarget()).as(proxyType).isSameAs(target);
			assertThat(targetInfo.getTargetClass()).as(proxyType).isEqualTo(TargetRegistry.class);
			assertThat(targetInfo.isDynamicTarget()).as(proxyType).isFalse();
			assertThat(targetInfo.isHotSwappable()).as(proxyType).isEqualTo("jdk-hot-swap".equals(proxyType));
		}
	}

	private static final class CountingTargetSource implements TargetSource {

		private final Object target;

		private final AtomicInteger activeCount = new AtomicInteger();

		private final AtomicInteger getCount = new AtomicInteger();

		private final AtomicInteger releaseCount = new AtomicInteger();

		private CountingTargetSource(Object target) {
			this.target = target;
		}

		private int getActiveCount() {
			return activeCount.get();
		}

		private int getGetCount() {
			return getCount.get();
		}

		private int getReleaseCount() {
			return releaseCount.get();
		}

		@Override
		public Class<?> getTargetClass() {
			return target.getClass();
		}

		@Override
		public boolean isStatic() {
			return false;
		}

		@Override
		public Object getTarget() {
			getCount.incrementAndGet();
			activeCount.incrementAndGet();
			return target;
		}

		@Override
		public void releaseTarget(Object target) {
			releaseCount.incrementAndGet();
			activeCount.decrementAndGet();
		}
	}

	public static class TargetRegistry implements ServiceRegistry<Registration> {

		@Override
		public void register(Registration registration) {
		}

		@Override
		public void deregister(Registration registration) {
		}

		@Override
		public void close() {
		}

		@Override
		public void setStatus(Registration registration, String status) {
		}

		@Override
		public Object getStatus(Registration registration) {
			return null;
		}
	}

	private static final class TargetRegistration implements Registration {

		@Override
		public String getServiceId() {
			return "test-service";
		}

		@Override
		public String getHost() {
			return "127.0.0.1";
		}

		@Override
		public int getPort() {
			return 48080;
		}

		@Override
		public boolean isSecure() {
			return false;
		}

		@Override
		public URI getUri() {
			return null;
		}

		@Override
		public Map<String, String> getMetadata() {
			return Map.of();
		}

		@Override
		public String getInstanceId() {
			return "test-instance";
		}
	}
}
