/*
 * Tencent is pleased to support the open source community by making Spring Cloud Tencent available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.rpc.enhancement.resttemplate;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

/**
 * Intercept for BlockingLoadBalancerClient, put host and port to thread local.
 *
 * @author lepdou 2022-09-05
 */
@Aspect
public class BlockingLoadBalancerClientAspect {

	@Pointcut("execution(public * org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient.reconstructURI(..)) ")
	public void pointcut() {

	}

	@Around("pointcut()")
	public Object invoke(ProceedingJoinPoint joinPoint) throws Throwable {
		LoadBalancerClientAspectUtils.extractLoadBalancerResult(joinPoint);
		return joinPoint.proceed();
	}
}
