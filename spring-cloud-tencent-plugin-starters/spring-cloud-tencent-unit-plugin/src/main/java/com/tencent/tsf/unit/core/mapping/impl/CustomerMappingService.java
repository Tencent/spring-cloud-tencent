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

package com.tencent.tsf.unit.core.mapping.impl;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.polaris.api.utils.StringUtils;
import com.tencent.tsf.unit.core.TencentUnitManager;
import com.tencent.tsf.unit.core.exception.ErrorCode;
import com.tencent.tsf.unit.core.exception.TencentUnitException;
import com.tencent.tsf.unit.core.mapping.api.IMappingService;
import com.tencent.tsf.unit.core.mapping.api.MappingEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shade.polaris.okhttp3.HttpUrl;
import shade.polaris.okhttp3.OkHttpClient;
import shade.polaris.okhttp3.Request;
import shade.polaris.okhttp3.Response;

public class CustomerMappingService implements IMappingService {

	private static final String DEFAULT_ROUTER_IDENTIFIER_QUERY_KEY = "RouterIdentifier";

	private static final Logger LOG = LoggerFactory.getLogger(CustomerMappingService.class);

	// 连接超时=2s,请求超时=2s
	private final OkHttpClient client = new OkHttpClient.Builder().
			connectTimeout(2, TimeUnit.SECONDS).readTimeout(2, TimeUnit.SECONDS).build();

	@Override
	public MappingEntity processMapping(String params) {
		String mappingUrl = getMappingUrl();
		if (StringUtils.isEmpty(mappingUrl)) {
			String msg = "[MappingService] mapping url is null";
			LOG.warn(msg);
			throw new TencentUnitException(ErrorCode.MAPPING_URL_EMPTY_ERROR, msg);
		}

		// params解析
		String url = urlBuild(mappingUrl, params);
		Request request = new Request.Builder().url(url).build();
		try {
			Response response = client.newCall(request).execute();
			if (!response.isSuccessful()) {
				String msg = String.format("request fail. url:%s, code:%s, err:%s",
						url, response.code(), response.message());
				LOG.error("[CustomerMappingService] " + msg);
				if (response.body() != null) {
					response.body().close();
				}
				throw new TencentUnitException(ErrorCode.MAPPING_RESPONSE_CODE_ERROR, msg);
			}

			if (response.body() == null) {
				String msg = String.format("response body is null. url:%s", url);
				LOG.error("[CustomerMappingService] " + msg);
				throw new TencentUnitException(ErrorCode.MAPPING_RESPONSE_EMPTY_BODY_ERROR, msg);
			}

			ObjectMapper objectMapper = new ObjectMapper();
			MappingEntity entity = objectMapper.readValue(response.body().string(), MappingEntity.class);
			response.body().close();
			return entity;
		}
		catch (IOException e) {
			String msg = String.format("occur IOException. url:%s, err:%s", url, e);
			if (LOG.isDebugEnabled()) {
				LOG.error("[CustomerMappingService] " + msg, e);
			}
			else {
				LOG.error("[CustomerMappingService] " + msg);
			}
			throw new TencentUnitException(ErrorCode.MAPPING_REQUEST_IO_ERROR, msg, e);
		}
	}

	private String urlBuild(String mappingUrl, String params) {
		if (params == null) {
			return mappingUrl;
		}

		HttpUrl.Builder urlBuilder = HttpUrl.get(mappingUrl).newBuilder();
		if (StringUtils.isNotEmpty(params)) {
			urlBuilder.addQueryParameter(DEFAULT_ROUTER_IDENTIFIER_QUERY_KEY, params);
		}
		return urlBuilder.build().toString();
	}

	/**
	 * 每次都从配置中获取.
	 */
	public String getMappingUrl() {
		return TencentUnitManager.getLocalCloudMappingApi();
	}
}
