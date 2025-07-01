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

package com.tencent.cloud.polaris.circuitbreaker.instrument.feign;

import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.polaris.api.utils.CollectionUtils;
import com.tencent.polaris.api.utils.StringUtils;
import feign.Request;
import feign.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.cloud.openfeign.CircuitBreakerNameResolver;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;

/**
 * PolarisCircuitBreakerNameResolver.
 *
 * @author seanyu 2023-02-27
 */
public class PolarisCircuitBreakerNameResolver implements CircuitBreakerNameResolver {

	private static final Logger LOG = LoggerFactory.getLogger(PolarisCircuitBreakerNameResolver.class);

	@Override
	public String resolveCircuitBreakerName(String feignClientName, Target<?> target, Method method) {
		String serviceName = target.name();
		String path = "";

		// Get path in @FeignClient.
		if (StringUtils.isNotBlank(target.url())) {
			URI uri = null;
			try {
				uri = new URI(target.url());
			}
			catch (URISyntaxException e) {
				LOG.warn("Generate URI from url({}) in @FeignClient. failed.", target.url());
			}
			if (uri != null) {
				path += uri.getPath();
			}
		}

		// Get path and method in @RequestMapping.
		RequestMapping requestMapping = findMergedAnnotation(method, RequestMapping.class);
		String httpMethod = Request.HttpMethod.GET.name();
		if (requestMapping != null) {
			path += requestMapping.path().length == 0 ?
					requestMapping.value().length == 0 ? "" : requestMapping.value()[0] :
					requestMapping.path()[0];

			RequestMethod[] requestMethods = requestMapping.method();
			if (CollectionUtils.isNotEmpty(requestMethods)) {
				httpMethod = requestMethods[0].name();
			}
		}


		return StringUtils.isBlank(path) ?
				MetadataContext.LOCAL_NAMESPACE + "#" + serviceName :
				MetadataContext.LOCAL_NAMESPACE + "#" + serviceName + "#" + path + "#http#" + httpMethod;
	}

}
