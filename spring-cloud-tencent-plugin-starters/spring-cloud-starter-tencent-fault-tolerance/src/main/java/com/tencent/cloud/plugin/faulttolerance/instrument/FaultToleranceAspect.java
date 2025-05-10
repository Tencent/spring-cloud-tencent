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

package com.tencent.cloud.plugin.faulttolerance.instrument;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.tencent.cloud.plugin.faulttolerance.annotation.FaultTolerance;
import com.tencent.cloud.plugin.faulttolerance.common.FallbackMethod;
import com.tencent.cloud.plugin.faulttolerance.model.FaultToleranceStrategy;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.tsf.faulttolerance.annotation.TsfFaultTolerance;
import org.springframework.cloud.tsf.faulttolerance.model.TsfFaultToleranceStragety;

/**
 * Fault tolerance.
 *
 * @author zhixinzxliu, Haotian Zhang
 */
@Aspect
public class FaultToleranceAspect {

	private static final Logger logger = LoggerFactory.getLogger(FaultToleranceAspect.class);

	private final Map<Method, FallbackMethod> fallbackMethodMap = new ConcurrentHashMap<>();
	private final ExecutorService executorService = Executors.newCachedThreadPool();

	@Pointcut("@annotation(org.springframework.cloud.tsf.faulttolerance.annotation.TsfFaultTolerance)"
			+ " || @annotation(com.tencent.cloud.plugin.faulttolerance.annotation.FaultTolerance)")
	public void faultToleranceAnnotationPointcut() {
	}

	@Around("faultToleranceAnnotationPointcut()")
	public Object methodsAnnotatedWithFaultTolerance(final ProceedingJoinPoint joinPoint) throws Throwable {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		FaultTolerance faultTolerance = signature.getMethod().getAnnotation(FaultTolerance.class);
		TsfFaultTolerance tsfFaultTolerance = signature.getMethod().getAnnotation(TsfFaultTolerance.class);

		Object result;
		try {
			// Invoke job in parallel. Whoever returns the result first will use it.
			// The latter result will be ignored directly.
			// If all exceptions occur, an exception will be thrown.
			if (faultTolerance != null
					&& faultTolerance.strategy() == FaultToleranceStrategy.FORKING
					&& faultTolerance.parallelism() > 1) {
				List<Callable<Object>> jobs = generateJobs(faultTolerance.parallelism(), joinPoint);
				result = executorService.invokeAny(jobs);
			}
			else if (tsfFaultTolerance != null &&
					tsfFaultTolerance.strategy() == TsfFaultToleranceStragety.FORKING &&
					tsfFaultTolerance.parallelism() > 1) {
				List<Callable<Object>> jobs = generateJobs(tsfFaultTolerance.parallelism(), joinPoint);
				result = executorService.invokeAny(jobs);
			}
			else {
				result = joinPoint.proceed();
			}
		}
		catch (Throwable throwable) {
			return execFaultToleranceLogic(joinPoint, faultTolerance, tsfFaultTolerance, throwable);
		}

		return result;
	}

	private Object execFaultToleranceLogic(ProceedingJoinPoint joinPoint, FaultTolerance faultTolerance, TsfFaultTolerance tsfFaultTolerance, Throwable throwable) throws Throwable {
		if (!needExecuteFaultTolerance(faultTolerance, tsfFaultTolerance, throwable)) {
			throw throwable;
		}

		// 重试逻辑
		if ((faultTolerance != null && faultTolerance.strategy() == FaultToleranceStrategy.FAIL_OVER)
				|| (tsfFaultTolerance != null && tsfFaultTolerance.strategy() == TsfFaultToleranceStragety.FAIL_OVER)) {
			int maxAttempts = faultTolerance != null ? faultTolerance.maxAttempts() : tsfFaultTolerance.maxAttempts();
			while (maxAttempts > 0) {
				try {
					return joinPoint.proceed();
				}
				catch (Throwable throwable1) {
					if (!needExecuteFaultTolerance(faultTolerance, tsfFaultTolerance, throwable1)) {
						throw throwable1;
					}

					logger.error("The {} time retry error, will continue retry {} times.", maxAttempts, maxAttempts - 1, throwable1);
				}
				finally {
					maxAttempts--;
				}
			}
		}

		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		Method method = signature.getMethod();
		if (method == null) {
			throw throwable;
		}

		FallbackMethod fallbackMethod = fallbackMethodMap.get(method);
		if (fallbackMethod == null) {
			fallbackMethod = resolveFallbackMethod(joinPoint);
			fallbackMethodMap.putIfAbsent(method, fallbackMethod);
		}
		if (fallbackMethod.getMethod() != null) {
			return fallbackMethod.getMethod().invoke(joinPoint.getTarget(), joinPoint.getArgs());
		}

		throw throwable;
	}

	private List<Callable<Object>> generateJobs(int parallelism, final ProceedingJoinPoint joinPoint) {
		List<Callable<Object>> jobs = new ArrayList<>();

		for (int i = 0; i < parallelism; i++) {
			jobs.add(() -> {
				try {
					return joinPoint.proceed();
				}
				catch (Throwable t) {
					throw new RuntimeException(t);
				}
			});
		}

		return jobs;
	}

	/**
	 * 1. If the user sets ignoreExceptions and the current exception is a subclass of one of them,
	 * the fault-tolerant logic will not be executed.<br>
	 * 2. If the user does not set ignoreExceptions or the current exception is not a subclass of ignoreExceptions
	 * and meets the following conditions, fault-tolerant logic will be executed:<br>
	 * 2.1. If the user does not set raisedExceptions, fault tolerance logic will be executed.<br>
	 * 2.2. The user has set raisedExceptions, and the current exception is a subclass of one of the raisedExceptions set by the user.
	 */
	private boolean needExecuteFaultTolerance(FaultTolerance faultTolerance, TsfFaultTolerance tsfFaultTolerance, Throwable throwable) {
		if (faultTolerance == null && tsfFaultTolerance == null) {
			return false;
		}
		Class<? extends Throwable>[] ignoreExceptions = faultTolerance != null ? faultTolerance.ignoreExceptions() : null;
		if (ignoreExceptions == null || ignoreExceptions.length == 0) {
			ignoreExceptions = tsfFaultTolerance != null ? tsfFaultTolerance.ignoreExceptions() : null;
		}
		Class<? extends Throwable>[] raisedExceptions = faultTolerance != null ? faultTolerance.raisedExceptions() : null;
		if (raisedExceptions == null || raisedExceptions.length == 0) {
			raisedExceptions = tsfFaultTolerance != null ? tsfFaultTolerance.raisedExceptions() : null;
		}

		try {

			if (ignoreExceptions != null) {
				for (Class<? extends Throwable> ignoreException : ignoreExceptions) {
					if (ignoreException.isAssignableFrom(throwable.getClass())) {
						logger.debug("The exception {} is ignored.", throwable.getClass().getName());
						return false;
					}
				}
			}

			if (raisedExceptions == null || raisedExceptions.length == 0) {
				return true;
			}
			else {
				for (Class<? extends Throwable> raisedException : raisedExceptions) {
					if (raisedException.isAssignableFrom(throwable.getClass())) {
						return true;
					}
				}
			}
		}
		catch (Throwable throwable1) {
			logger.warn("Check exception {} failed.", throwable.getClass().getName(), throwable1);
			return false;
		}

		return false;
	}

	protected FallbackMethod resolveFallbackMethod(ProceedingJoinPoint joinPoint) {
		MethodSignature signature = (MethodSignature) joinPoint.getSignature();
		FaultTolerance faultTolerance = signature.getMethod().getAnnotation(FaultTolerance.class);
		TsfFaultTolerance tsfFaultTolerance = signature.getMethod().getAnnotation(TsfFaultTolerance.class);

		String fallbackMethodName = faultTolerance != null ? faultTolerance.fallbackMethod() : tsfFaultTolerance.fallbackMethod();
		Class<?> targetClass = joinPoint.getTarget().getClass();
		Class<?>[] parameterTypes = signature.getMethod().getParameterTypes();

		Method method = getDeclaredMethodFor(targetClass, fallbackMethodName, false, parameterTypes);

		if (method == null) {
			return FallbackMethod.ABSENT;
		}
		else {
			if (signature.getMethod().getReturnType().isAssignableFrom(method.getReturnType())) {
				return new FallbackMethod(method);
			}
			else {
				return FallbackMethod.ABSENT;
			}
		}
	}

	private Method getDeclaredMethodFor(Class<?> clazz, String name, boolean inRecursive, Class<?>... parameterTypes) {
		try {
			return clazz.getDeclaredMethod(name, parameterTypes);
		}
		catch (NoSuchMethodException e) {
			if (!inRecursive) {
				logger.warn("Fallback method not found! Classname = {}, methodName = {}, parameterTypes = {}",
						clazz.getName(), name, Arrays.toString(parameterTypes));
			}
			Class<?> superClass = clazz.getSuperclass();
			if (superClass != null) {
				return getDeclaredMethodFor(superClass, name, true, parameterTypes);
			}
		}
		return null;
	}
}
