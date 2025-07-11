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

package com.tencent.cloud.tsf.demo.consumer.proxy;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * 测试通过URL配置FeignClient
 * 使用时修改provider-ip:provider-port配置
 */
@FeignClient(name = "provider", url = "http://127.0.0.1:18081", fallback = FeignClientFallback.class)
public interface ProviderService {

	@RequestMapping(value = "/echo/{str}", method = RequestMethod.GET)
	String echo(@PathVariable("str") String str);

}

@Component
class FeignClientFallback implements ProviderService {
	@Override
	public String echo(String str) {
		return "tsf-fault-tolerance-" + str;
	}
}
