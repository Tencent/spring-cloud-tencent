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

package com.tencent.cloud.rpc.enhancement.feign;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.tencent.cloud.rpc.enhancement.feign.plugin.EnhancedFeignContext;
import com.tencent.cloud.rpc.enhancement.feign.plugin.EnhancedFeignPlugin;
import com.tencent.cloud.rpc.enhancement.feign.plugin.EnhancedFeignPluginType;
import feign.Client;
import feign.Request;
import feign.Request.Options;
import feign.Response;

import org.springframework.util.CollectionUtils;

import static feign.Util.checkNotNull;

/**
 * Wrap for {@link Client}.
 *
 * @author Haotian Zhang
 */
public class EnhancedFeignClient implements Client {

	private final Client delegate;

	private List<EnhancedFeignPlugin> preEnhancedFeignPlugins;

	private List<EnhancedFeignPlugin> postEnhancedFeignPlugins;

	private List<EnhancedFeignPlugin> exceptionEnhancedFeignPlugins;

	private List<EnhancedFeignPlugin> finallyEnhancedFeignPlugins;

	public EnhancedFeignClient(Client target, List<EnhancedFeignPlugin> enhancedFeignPlugins) {
		this.delegate = checkNotNull(target, "target");

		// Init the EnhancedFeignPlugins list.
		this.preEnhancedFeignPlugins = new ArrayList<>();
		this.postEnhancedFeignPlugins = new ArrayList<>();
		this.exceptionEnhancedFeignPlugins = new ArrayList<>();
		this.finallyEnhancedFeignPlugins = new ArrayList<>();
		if (!CollectionUtils.isEmpty(enhancedFeignPlugins)) {
			for (EnhancedFeignPlugin feignPlugin : enhancedFeignPlugins) {
				if (feignPlugin.getType().equals(EnhancedFeignPluginType.PRE)) {
					this.preEnhancedFeignPlugins.add(feignPlugin);
				}
				else if (feignPlugin.getType().equals(EnhancedFeignPluginType.POST)) {
					this.postEnhancedFeignPlugins.add(feignPlugin);
				}
				else if (feignPlugin.getType().equals(EnhancedFeignPluginType.EXCEPTION)) {
					this.exceptionEnhancedFeignPlugins.add(feignPlugin);
				}
				else if (feignPlugin.getType().equals(EnhancedFeignPluginType.FINALLY)) {
					this.finallyEnhancedFeignPlugins.add(feignPlugin);
				}
			}
		}
		// Set the ordered enhanced feign plugins.
		this.preEnhancedFeignPlugins = getSortedEnhancedFeignPlugin(this.preEnhancedFeignPlugins);
		this.postEnhancedFeignPlugins = getSortedEnhancedFeignPlugin(this.postEnhancedFeignPlugins);
		this.exceptionEnhancedFeignPlugins = getSortedEnhancedFeignPlugin(this.exceptionEnhancedFeignPlugins);
		this.finallyEnhancedFeignPlugins = getSortedEnhancedFeignPlugin(this.finallyEnhancedFeignPlugins);
	}

	@Override
	public Response execute(Request request, Options options) throws IOException {
		EnhancedFeignContext enhancedFeignContext = new EnhancedFeignContext();
		enhancedFeignContext.setRequest(request);
		enhancedFeignContext.setOptions(options);

		// Run pre enhanced feign plugins.
		for (EnhancedFeignPlugin plugin : preEnhancedFeignPlugins) {
			try {
				plugin.run(enhancedFeignContext);
			}
			catch (Throwable throwable) {
				plugin.handlerThrowable(enhancedFeignContext, throwable);
			}
		}
		try {
			Response response = delegate.execute(request, options);
			enhancedFeignContext.setResponse(response);

			// Run post enhanced feign plugins.
			for (EnhancedFeignPlugin plugin : postEnhancedFeignPlugins) {
				try {
					plugin.run(enhancedFeignContext);
				}
				catch (Throwable throwable) {
					plugin.handlerThrowable(enhancedFeignContext, throwable);
				}
			}
			return response;
		}
		catch (IOException origin) {
			enhancedFeignContext.setException(origin);
			// Run exception enhanced feign plugins.
			for (EnhancedFeignPlugin plugin : exceptionEnhancedFeignPlugins) {
				try {
					plugin.run(enhancedFeignContext);
				}
				catch (Throwable throwable) {
					plugin.handlerThrowable(enhancedFeignContext, throwable);
				}
			}
			throw origin;
		}
		finally {
			// Run finally enhanced feign plugins.
			for (EnhancedFeignPlugin plugin : finallyEnhancedFeignPlugins) {
				try {
					plugin.run(enhancedFeignContext);
				}
				catch (Throwable throwable) {
					plugin.handlerThrowable(enhancedFeignContext, throwable);
				}
			}
		}
	}

	/**
	 * Ascending, which means the lower order number, the earlier executing enhanced feign plugin.
	 *
	 * @return sorted feign pre plugin list
	 */
	private List<EnhancedFeignPlugin> getSortedEnhancedFeignPlugin(List<EnhancedFeignPlugin> preEnhancedFeignPlugins) {
		return new ArrayList<>(preEnhancedFeignPlugins)
				.stream()
				.sorted(Comparator.comparing(EnhancedFeignPlugin::getOrder))
				.collect(Collectors.toList());
	}
}
