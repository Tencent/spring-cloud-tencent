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

import com.tencent.cloud.rpc.enhancement.feign.plugin.EnhancedFeignContext;
import feign.Client;
import feign.Request;
import feign.Request.Options;
import feign.Response;

import static com.tencent.cloud.rpc.enhancement.feign.plugin.EnhancedFeignPluginType.EXCEPTION;
import static com.tencent.cloud.rpc.enhancement.feign.plugin.EnhancedFeignPluginType.FINALLY;
import static com.tencent.cloud.rpc.enhancement.feign.plugin.EnhancedFeignPluginType.POST;
import static com.tencent.cloud.rpc.enhancement.feign.plugin.EnhancedFeignPluginType.PRE;
import static feign.Util.checkNotNull;

/**
 * Wrap for {@link Client}.
 *
 * @author Haotian Zhang
 */
public class EnhancedFeignClient implements Client {

	private final Client delegate;

	private EnhancedFeignPluginRunner pluginRunner;

	public EnhancedFeignClient(Client target, EnhancedFeignPluginRunner pluginRunner) {
		this.delegate = checkNotNull(target, "target");
		this.pluginRunner = pluginRunner;
	}

	@Override
	public Response execute(Request request, Options options) throws IOException {
		EnhancedFeignContext enhancedFeignContext = new EnhancedFeignContext();
		enhancedFeignContext.setRequest(request);
		enhancedFeignContext.setOptions(options);

		// Run pre enhanced feign plugins.
		pluginRunner.run(PRE, enhancedFeignContext);
		try {
			Response response = delegate.execute(request, options);
			enhancedFeignContext.setResponse(response);

			// Run post enhanced feign plugins.
			pluginRunner.run(POST, enhancedFeignContext);
			return response;
		}
		catch (IOException origin) {
			enhancedFeignContext.setException(origin);
			// Run exception enhanced feign plugins.
			pluginRunner.run(EXCEPTION, enhancedFeignContext);
			throw origin;
		}
		finally {
			// Run finally enhanced feign plugins.
			pluginRunner.run(FINALLY, enhancedFeignContext);
		}
	}
}
