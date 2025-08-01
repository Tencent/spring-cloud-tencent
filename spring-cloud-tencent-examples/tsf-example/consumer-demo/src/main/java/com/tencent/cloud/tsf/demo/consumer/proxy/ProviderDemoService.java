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

import com.tencent.cloud.tsf.demo.consumer.entity.User;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "${provider.name:http://provider-demo}")
public interface ProviderDemoService {
	@RequestMapping(value = "/echo/{str}", method = RequestMethod.GET)
	String echo(@PathVariable("str") String str);

	@RequestMapping(value = "/echo/error/{str}", method = RequestMethod.GET)
	String echoError(@PathVariable("str") String str);

	@RequestMapping(value = "/echo/slow/{str}", method = RequestMethod.GET)
	String echoSlow(@PathVariable("str") String str, @RequestParam("delay") int delay);

	@RequestMapping(value = "/user", method = RequestMethod.POST)
	String user(@RequestBody User user);
}
