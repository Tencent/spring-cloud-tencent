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

package com.tencent.cloud.common.metadata.aop;

import java.net.URI;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import feign.Request;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;

/**
 * Aspect for getting service name of peer-service in Feign of Greenwich.
 *
 * @author Haotian Zhang
 */
@Aspect
public class MetadataFeignAspect {

	@Pointcut("execution(* feign.Client.execute(..))")
	public void execute() {
	}

	/**
	 * Get service name before execution of Feign client.
	 *
	 * @param joinPoint join point
	 */
	@Before("execute()")
	public void doBefore(JoinPoint joinPoint) {
		Request request = (Request) joinPoint.getArgs()[0];
		MetadataContextHolder.get().putSystemMetadata(MetadataConstant.SystemMetadataKey.PEER_SERVICE,
				URI.create(request.url()).getAuthority());
	}
}
