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

package com.tencent.cloud.polaris.router.feign;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import com.netflix.loadbalancer.reactive.LoadBalancerCommand;
import com.tencent.cloud.common.constant.RouterConstants;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.polaris.router.PolarisRouterContext;

import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.openfeign.ribbon.FeignLoadBalancer;
import org.springframework.util.CollectionUtils;

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;

/**
 * In order to pass router context for {@link com.tencent.cloud.polaris.router.PolarisLoadBalancerCompositeRule}.
 *
 * @author lepdou, cheese8
 */
public class PolarisFeignLoadBalancer extends FeignLoadBalancer {

	public PolarisFeignLoadBalancer(ILoadBalancer lb, IClientConfig clientConfig, ServerIntrospector serverIntrospector) {
		super(lb, clientConfig, serverIntrospector);
	}

	@Override
	protected void customizeLoadBalancerCommandBuilder(RibbonRequest request, IClientConfig config,
			LoadBalancerCommand.Builder<RibbonResponse> builder) {
		Map<String, Collection<String>> headers = request.getRequest().headers();

		PolarisRouterContext routerContext = buildRouterContext(headers);

		builder.withServerLocator(routerContext);
	}

	//set method to public for unit test
	PolarisRouterContext buildRouterContext(Map<String, Collection<String>> headers) {
		Collection<String> labelHeaderValues = headers.get(RouterConstants.ROUTER_LABEL_HEADER);

		if (CollectionUtils.isEmpty(labelHeaderValues)) {
			return null;
		}

		PolarisRouterContext routerContext = new PolarisRouterContext();

		routerContext.putLabels(PolarisRouterContext.TRANSITIVE_LABELS, MetadataContextHolder.get()
				.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE));

		Map<String, String> labelHeaderValuesMap = new HashMap<>();
		try {
			Optional<String> opt = labelHeaderValues.stream().findFirst();
			if (opt.isPresent()) {
				String labelHeaderValuesContent = opt.get();
				Map<String, String> labels = JacksonUtils.deserialize2Map(URLDecoder.decode(labelHeaderValuesContent, UTF_8));
				labelHeaderValuesMap.putAll(labels);
			}
		}
		catch (UnsupportedEncodingException e) {
			throw new RuntimeException("unsupported charset exception " + UTF_8);
		}
		routerContext.putLabels(PolarisRouterContext.ROUTER_LABELS, labelHeaderValuesMap);

		return routerContext;
	}
}
