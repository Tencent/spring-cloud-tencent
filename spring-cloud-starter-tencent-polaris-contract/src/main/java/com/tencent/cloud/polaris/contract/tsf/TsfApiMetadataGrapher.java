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

package com.tencent.cloud.polaris.contract.tsf;

import java.util.concurrent.atomic.AtomicBoolean;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.cloud.common.util.GzipUtil;
import io.swagger.v3.oas.models.OpenAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.api.AbstractOpenApiResource;
import org.springdoc.api.AbstractOpenApiResourceUtil;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springdoc.webflux.api.OpenApiWebFluxUtil;
import org.springdoc.webmvc.api.OpenApiWebMvcUtil;

import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

public class TsfApiMetadataGrapher implements SmartLifecycle {

	private static final Logger logger = LoggerFactory.getLogger(TsfApiMetadataGrapher.class);
	private final AtomicBoolean isRunning = new AtomicBoolean(false);
	private final org.springdoc.webmvc.api.MultipleOpenApiResource multipleOpenApiWebMvcResource;
	private final org.springdoc.webflux.api.MultipleOpenApiResource multipleOpenApiWebFluxResource;
	private final ObjectMapperProvider springdocObjectMapperProvider;
	private final ApplicationContext applicationContext;
	private final String groupName;

	public TsfApiMetadataGrapher(org.springdoc.webmvc.api.MultipleOpenApiResource multipleOpenApiWebMvcResource,
			org.springdoc.webflux.api.MultipleOpenApiResource multipleOpenApiWebFluxResource,
			String groupName, ApplicationContext applicationContext, ObjectMapperProvider springdocObjectMapperProvider) {
		this.applicationContext = applicationContext;
		this.multipleOpenApiWebMvcResource = multipleOpenApiWebMvcResource;
		this.multipleOpenApiWebFluxResource = multipleOpenApiWebFluxResource;
		this.groupName = groupName;
		this.springdocObjectMapperProvider = springdocObjectMapperProvider;
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void stop(Runnable runnable) {
		runnable.run();
		stop();
	}

	@Override
	public void start() {
		if (!isRunning.compareAndSet(false, true)) {
			return;
		}
		try {
			AbstractOpenApiResource openApiResource = null;
			if (multipleOpenApiWebMvcResource != null) {
				openApiResource = OpenApiWebMvcUtil.getOpenApiResourceOrThrow(multipleOpenApiWebMvcResource, groupName);
			}
			else if (multipleOpenApiWebFluxResource != null) {
				openApiResource = OpenApiWebFluxUtil.getOpenApiResourceOrThrow(multipleOpenApiWebFluxResource, groupName);
			}
			OpenAPI openAPI = null;
			if (openApiResource != null) {
				openAPI = AbstractOpenApiResourceUtil.getOpenApi(openApiResource);
			}
			String jsonValue;
			if (springdocObjectMapperProvider != null && springdocObjectMapperProvider.jsonMapper() != null) {
				jsonValue = springdocObjectMapperProvider.jsonMapper().writeValueAsString(openAPI);
			}
			else {
				ObjectMapper mapper = new ObjectMapper();
				mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
				jsonValue = mapper.writeValueAsString(openAPI);
			}
			if (openAPI != null && !StringUtils.isEmpty(jsonValue)) {
				String serviceApiMeta = GzipUtil.compressBase64Encode(jsonValue, "utf-8");
				Environment environment = applicationContext.getEnvironment();
				String tsfToken = environment.getProperty("tsf_token");
				String tsfGroupId = environment.getProperty("tsf_group_id");
				if (StringUtils.isEmpty(tsfGroupId) || StringUtils.isEmpty(tsfToken)) {
					logger.info("[tsf-swagger] auto smart check application start with local consul, api registry not work");
					return;
				}
				logger.info("[tsf-swagger] api_meta len: {}", serviceApiMeta.length());
				String applicationName = environment.getProperty("spring.application.name");
				if (logger.isDebugEnabled()) {
					logger.debug("[tsf-swagger] service: {} openApi json data: {}", applicationName, jsonValue);
					logger.debug("[tsf-swagger] service: {} api_meta info: {}", applicationName, serviceApiMeta);
				}

				System.setProperty(String.format("$%s", "api_metas"), serviceApiMeta);
			}
			else {
				logger.warn("[tsf-swagger] swagger or json is null, openApiResource keys:{}, group:{}", openApiResource, groupName);
			}
		}
		catch (Throwable t) {
			logger.error("[tsf swagger] init TsfApiMetadataGrapher failed. occur exception: ", t);
		}
	}

	@Override
	public void stop() {
		isRunning.set(true);
	}

	@Override
	public boolean isRunning() {
		return isRunning.get();
	}

	@Override
	public int getPhase() {
		return -2;
	}
}
