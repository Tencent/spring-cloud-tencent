/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.polaris.circuitbreaker.instrument.feign;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.LinkedHashMap;
import java.util.Map;

import com.tencent.cloud.polaris.circuitbreaker.exception.FallbackWrapperException;
import feign.InvocationHandlerFactory;
import feign.Target;
import feign.codec.Decoder;

import org.springframework.cloud.client.circuitbreaker.NoFallbackAvailableException;
import org.springframework.cloud.openfeign.FallbackFactory;

import static feign.Util.checkNotNull;

/**
 * PolarisFeignCircuitBreakerInvocationHandler, mostly copy from {@link org.springframework.cloud.openfeign.FeignCircuitBreakerInvocationHandler}, but giving Polaris modification.
 *
 * @author sean yu
 */
public class PolarisFeignCircuitBreakerInvocationHandler implements InvocationHandler {
	private final Target<?> target;

	private final Map<Method, InvocationHandlerFactory.MethodHandler> dispatch;

	private final FallbackFactory<?> nullableFallbackFactory;

	private final Map<Method, Method> fallbackMethodMap;

	private final Decoder decoder;

	public PolarisFeignCircuitBreakerInvocationHandler(Target<?> target,
			Map<Method, InvocationHandlerFactory.MethodHandler> dispatch,
			FallbackFactory<?> nullableFallbackFactory, Decoder decoder) {

		this.target = checkNotNull(target, "target");
		this.dispatch = checkNotNull(dispatch, "dispatch");
		this.fallbackMethodMap = toFallbackMethod(dispatch);
		this.nullableFallbackFactory = nullableFallbackFactory;
		this.decoder = decoder;
	}

	/**
	 * If the method param of {@link InvocationHandler#invoke(Object, Method, Object[])}
	 * is not accessible, i.e in a package-private interface, the fallback call will cause
	 * of access restrictions. But methods in dispatch are copied methods. So setting
	 * access to dispatch method doesn't take effect to the method in
	 * InvocationHandler.invoke. Use map to store a copy of method to invoke the fallback
	 * to bypass this and reducing the count of reflection calls.
	 * @return cached methods map for fallback invoking
	 */
	static Map<Method, Method> toFallbackMethod(Map<Method, InvocationHandlerFactory.MethodHandler> dispatch) {
		Map<Method, Method> result = new LinkedHashMap<>();
		for (Method method : dispatch.keySet()) {
			method.setAccessible(true);
			result.put(method, method);
		}
		return result;
	}

	@Override
	public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
		// early exit if the invoked method is from java.lang.Object
		// code is the same as ReflectiveFeign.FeignInvocationHandler
		if ("equals".equals(method.getName())) {
			try {
				Object otherHandler = args.length > 0 && args[0] != null ? Proxy.getInvocationHandler(args[0]) : null;
				return equals(otherHandler);
			}
			catch (IllegalArgumentException e) {
				return false;
			}
		}
		else if ("hashCode".equals(method.getName())) {
			return hashCode();
		}
		else if ("toString".equals(method.getName())) {
			return toString();
		}

		try {
			return this.dispatch.get(method).invoke(args);
		}
		catch (Exception e) {
			if (this.nullableFallbackFactory != null) {
				Object fallback = this.nullableFallbackFactory.create(e);
				try {
					return this.fallbackMethodMap.get(method).invoke(fallback, args);
				}
				catch (Exception exception) {
					unwrapAndRethrow(exception);
				}
				return null;
			}
			else {
				PolarisCircuitBreakerFallbackFactory.DefaultFallback fallback =
						(PolarisCircuitBreakerFallbackFactory.DefaultFallback) new PolarisCircuitBreakerFallbackFactory(this.decoder).create(e);
				return fallback.fallback(method);
			}
		}
	}

	private void unwrapAndRethrow(Exception exception) {
		if (exception instanceof InvocationTargetException || exception instanceof NoFallbackAvailableException) {
			Throwable underlyingException = exception.getCause();
			if (underlyingException instanceof RuntimeException) {
				throw (RuntimeException) underlyingException;
			}
			if (underlyingException != null) {
				throw new FallbackWrapperException(underlyingException);
			}
			throw new FallbackWrapperException(exception);
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PolarisFeignCircuitBreakerInvocationHandler) {
			PolarisFeignCircuitBreakerInvocationHandler other = (PolarisFeignCircuitBreakerInvocationHandler) obj;
			return this.target.equals(other.target);
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.target.hashCode();
	}

	@Override
	public String toString() {
		return this.target.toString();
	}

}
