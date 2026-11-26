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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.IdentityHashMap;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.target.HotSwappableTargetSource;
import org.springframework.cglib.proxy.Factory;

/**
 * Resolver for AOP proxies used by lossless registration and deregistration.
 */
public final class AopTargetUtils {

	private static final String JDK_DYNAMIC_AOP_PROXY_CLASS_NAME =
			"org.springframework.aop.framework.JdkDynamicAopProxy";

	private static final String CGLIB_DYNAMIC_ADVISED_INTERCEPTOR_CLASS_NAME =
			"org.springframework.aop.framework.CglibAopProxy$DynamicAdvisedInterceptor";

	private static final String ADVISED_FIELD_NAME = "advised";

	private AopTargetUtils() {
	}

	static TargetInfo resolveTarget(Object candidate) {
		Object current = candidate;
		boolean hotSwappable = false;
		IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
		while (current != null && visited.put(current, Boolean.TRUE) == null) {
			if (current instanceof Advised advised) {
				TargetSource targetSource = advised.getTargetSource();
				if (targetSource instanceof HotSwappableTargetSource) {
					hotSwappable = true;
				}
				current = resolveTargetSource(targetSource);
				if (current == null) {
					return new TargetInfo(null, targetSource.getTargetClass(), hotSwappable, true);
				}
				continue;
			}
			if (Proxy.isProxyClass(current.getClass())) {
				TargetSource targetSource = getJdkDynamicAopTargetSource(current);
				if (targetSource != null) {
					if (targetSource instanceof HotSwappableTargetSource) {
						hotSwappable = true;
					}
					Object target = resolveTargetSource(targetSource);
					if (target == null) {
						return new TargetInfo(null, targetSource.getTargetClass(), hotSwappable, true);
					}
					if (target != current) {
						current = target;
						continue;
					}
				}
			}
			if (current instanceof Factory factory) {
				TargetSource targetSource = getCglibAopTargetSource(factory);
				if (targetSource != null) {
					if (targetSource instanceof HotSwappableTargetSource) {
						hotSwappable = true;
					}
					Object target = resolveTargetSource(targetSource);
					if (target == null) {
						return new TargetInfo(null, targetSource.getTargetClass(), hotSwappable, true);
					}
					if (target != current) {
						current = target;
						continue;
					}
				}
			}
			break;
		}
		return new TargetInfo(current, current == null ? null : current.getClass(), hotSwappable, false);
	}

	public static Object getUltimateTarget(Object candidate) {
		return resolveTarget(candidate).getTarget();
	}

	public static Class<?> getUltimateTargetClass(Object candidate) {
		return resolveTarget(candidate).getTargetClass();
	}

	private static Object resolveTargetSource(TargetSource targetSource) {
		if (targetSource instanceof HotSwappableTargetSource || targetSource.isStatic()) {
			return getTarget(targetSource);
		}
		return null;
	}

	private static TargetSource getCglibAopTargetSource(Factory proxy) {
		try {
			Object callback = proxy.getCallback(0);
			if (callback == null) {
				return null;
			}
			Class<?> callbackClass = callback.getClass();
			if (!CGLIB_DYNAMIC_ADVISED_INTERCEPTOR_CLASS_NAME.equals(callbackClass.getName())) {
				return null;
			}
			Field advisedField = callbackClass.getDeclaredField(ADVISED_FIELD_NAME);
			advisedField.setAccessible(true);
			Object advised = advisedField.get(callback);
			if (advised instanceof AdvisedSupport advisedSupport) {
				return advisedSupport.getTargetSource();
			}
		}
		catch (Exception e) {
			throw new IllegalStateException("Failed to resolve the CGLIB AOP proxy target.", e);
		}
		return null;
	}

	private static Object getTarget(TargetSource targetSource) {
		try {
			return targetSource.getTarget();
		}
		catch (Exception e) {
			throw new IllegalStateException("Failed to resolve the Spring AOP target.", e);
		}
	}

	private static TargetSource getJdkDynamicAopTargetSource(Object proxy) {
		try {
			InvocationHandler invocationHandler = Proxy.getInvocationHandler(proxy);
			Class<?> invocationHandlerClass = invocationHandler.getClass();
			if (!JDK_DYNAMIC_AOP_PROXY_CLASS_NAME.equals(invocationHandlerClass.getName())) {
				return null;
			}
			Field advisedField = invocationHandlerClass.getDeclaredField(ADVISED_FIELD_NAME);
			advisedField.setAccessible(true);
			Object advised = advisedField.get(invocationHandler);
			if (advised instanceof AdvisedSupport advisedSupport) {
				return advisedSupport.getTargetSource();
			}
		}
		catch (Exception e) {
			throw new IllegalStateException("Failed to resolve the JDK AOP proxy target.", e);
		}
		return null;
	}

	static final class TargetInfo {

		private final Object target;

		private final boolean hotSwappable;

		private final boolean dynamicTarget;

		private final Class<?> targetClass;

		private TargetInfo(Object target, Class<?> targetClass, boolean hotSwappable, boolean dynamicTarget) {
			this.target = target;
			this.hotSwappable = hotSwappable;
			this.dynamicTarget = dynamicTarget;
			this.targetClass = targetClass;
		}

		Object getTarget() {
			return target;
		}

		boolean isHotSwappable() {
			return hotSwappable;
		}

		boolean isDynamicTarget() {
			return dynamicTarget;
		}

		Class<?> getTargetClass() {
			return targetClass;
		}

	}

}
