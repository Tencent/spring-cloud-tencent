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

package com.tencent.cloud.polaris.circuitbreaker.feign;

import java.lang.reflect.Method;

import com.tencent.cloud.common.metadata.MetadataContext;
import feign.Target;

import org.springframework.web.bind.annotation.RequestMapping;

import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;

/**
 * PolarisCircuitBreakerNameResolver.
 *
 * @author seanyu 2023-02-27
 */
public class PolarisCircuitBreakerNameResolver {

	public String resolveCircuitBreakerName(String feignClientName, Target<?> target, Method method) {
		String serviceName = target.name();
		RequestMapping requestMapping = findMergedAnnotation(method, RequestMapping.class);
		String path = "";
		if (requestMapping != null) {
			path = requestMapping.path().length == 0 ?
					requestMapping.value().length == 0 ? "" : requestMapping.value()[0] :
					requestMapping.path()[0];
		}
		return "".equals(path) ?
				MetadataContext.LOCAL_NAMESPACE + "#" + serviceName :
				MetadataContext.LOCAL_NAMESPACE + "#" + serviceName + "#" + path;
	}
}
