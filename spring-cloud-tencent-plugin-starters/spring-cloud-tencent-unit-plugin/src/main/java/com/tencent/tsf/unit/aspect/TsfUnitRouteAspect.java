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

package com.tencent.tsf.unit.aspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import com.tencent.tsf.unit.annotation.TsfUnitCall;
import com.tencent.tsf.unit.annotation.TsfUnitCustomerIdentifier;
import com.tencent.tsf.unit.core.utils.TencentUnitUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 拦截单元化相关的注解.
 */
@Aspect
public class TsfUnitRouteAspect {

	private static final Logger LOG = LoggerFactory.getLogger(TsfUnitRouteAspect.class);

	public TsfUnitRouteAspect() {
		LOG.info("init TsfUnitRouteAspect");
	}

	// 拦截@FeignClient+@TsfUnitCall的对象
	@Pointcut("@within(com.tencent.tsf.unit.annotation.TsfUnitCall) &&" +
			"@within(org.springframework.cloud.openfeign.FeignClient)")
	public void unitRoutePointcut() {
	}

	@Around("unitRoutePointcut()")
	public Object unitRouteProcess(ProceedingJoinPoint joinPoint) throws Throwable {
		try {
			// 获取拦截点（pointcut）的函数签名
			MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
			// 获取拦截点的类名
			String pointClassName = methodSignature.getDeclaringTypeName();
			// 获取拦截点所属类名的Class
			Class<?> pointCutClass = Class.forName(pointClassName);
			// 获取拦截点所在类Class上的注解，获取本地调用注解@TsfUnitCall
			TsfUnitCall tsfUnitCallClass = pointCutClass.getAnnotation(TsfUnitCall.class);

			// 获取单元化注解上的systemName
			String systemName = tsfUnitCallClass.systemName();
			boolean global = tsfUnitCallClass.global();
			// 获取在拦截点对象上执行的方法
			Method pointCutMethod = methodSignature.getMethod();
			// 获取方法上所有参数的注解，找到@TsfUnitUserTag的修饰的参数，作为用户标签（客户号）取出来
			String cid = getCustomerIdentifier(pointCutMethod.getParameterAnnotations(), joinPoint.getArgs());

			// 写入Context
			TencentUnitUtils.putCustomerTags(systemName, cid, global);

			return joinPoint.proceed();
		}
		catch (Throwable e) {
			LOG.error("[TsfUnitRoute] aspect catch exception: " + e.getMessage());
			throw e;
		}
	}

	// 获取参数里面里有userTag相关注解的参数
	private String getCustomerIdentifier(Annotation[][] annotations, Object[] args) {
		// 找到TsfUnitUserTag注解的位置
		int index = -1;
		for (int i = 0; i < annotations.length; i++) {
			for (Annotation annotation : annotations[i]) {
				if (annotation.annotationType() == TsfUnitCustomerIdentifier.class) {
					index = i;
					break;
				}
			}
			if (index >= 0) {
				break;
			}
		}

		if (index >= 0 && index < args.length) {
			Object object = args[index];
			if (object.getClass().equals(String.class)) {
				return (String) object;
			}
		}

		return null;
	}
}
