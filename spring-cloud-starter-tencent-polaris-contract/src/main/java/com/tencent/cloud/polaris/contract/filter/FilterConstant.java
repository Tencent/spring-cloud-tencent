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

package com.tencent.cloud.polaris.contract.filter;

/**
 * Constant for filter.
 *
 * @author Haotian Zhang
 */
public final class FilterConstant {

	/**
	 * Swagger api doc V2 url.
	 */
	public static final String SWAGGER_V2_API_DOC_URL = "/v2/api-docs";

	/**
	 * Swagger api doc V3 url.
	 */
	public static final String SWAGGER_V3_API_DOC_URL = "/v3/api-docs";

	/**
	 * Swagger UI V2 url.
	 */
	public static final String SWAGGER_UI_V2_URL = "/swagger-ui.html";

	/**
	 * Swagger UI V3 url.
	 */
	public static final String SWAGGER_UI_V3_URL = "/swagger-ui/index.html";

	/**
	 * Swagger resource url prefix.
	 */
	public static final String SWAGGER_RESOURCE_PREFIX = "/swagger-resource/";

	/**
	 * Swagger webjars V2 url prefix.
	 */
	public static final String SWAGGER_WEBJARS_V2_PREFIX = "/webjars/springfox-swagger-ui/";

	/**
	 * Swagger webjars V3 url prefix.
	 */
	public static final String SWAGGER_WEBJARS_V3_PREFIX = "/webjars/swagger-ui/";

	private FilterConstant() {
	}
}
