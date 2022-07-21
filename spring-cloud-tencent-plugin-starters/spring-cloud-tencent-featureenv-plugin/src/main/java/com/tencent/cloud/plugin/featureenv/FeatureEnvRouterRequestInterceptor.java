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
 *
 */

package com.tencent.cloud.plugin.featureenv;

import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.polaris.router.PolarisRouterContext;
import com.tencent.cloud.polaris.router.spi.RouterRequestInterceptor;
import com.tencent.polaris.api.rpc.MetadataFailoverType;
import com.tencent.polaris.plugins.router.metadata.MetadataRouter;
import com.tencent.polaris.router.api.rpc.ProcessRoutersRequest;
import org.apache.commons.lang.StringUtils;

/**
 * Build metadata router context for feature env scene.
 * @author lepdou 2022-07-06
 */
public class FeatureEnvRouterRequestInterceptor implements RouterRequestInterceptor {

	private static final String LABEL_KEY_FEATURE_ENV_ROUTER_KEY = "system-feature-env-router-label";
	private static final String DEFAULT_FEATURE_ENV_ROUTER_LABEL = "featureenv";
	private static final String NOT_EXISTED_ENV = "NOT_EXISTED_ENV";

	@Override
	public void apply(ProcessRoutersRequest request, PolarisRouterContext routerContext) {
		//1. get feature env router label key
		String envLabelKey = routerContext.getLabel(LABEL_KEY_FEATURE_ENV_ROUTER_KEY);
		if (StringUtils.isBlank(envLabelKey)) {
			envLabelKey = DEFAULT_FEATURE_ENV_ROUTER_LABEL;
		}

		//2. get feature env router label value
		String envLabelValue = routerContext.getLabel(envLabelKey);
		if (envLabelValue == null) {
			// router to base env when not matched feature env
			envLabelValue = NOT_EXISTED_ENV;
		}

		//3. set env metadata to router request
		Map<String, String> envMetadata = new HashMap<>();
		envMetadata.put(envLabelKey, envLabelValue);

		request.addRouterMetadata(MetadataRouter.ROUTER_TYPE_METADATA, envMetadata);

		//4. set failover type to others
		request.setMetadataFailoverType(MetadataFailoverType.METADATAFAILOVERNOTKEY);
	}
}
