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

package com.tencent.cloud.polaris.circuitbreaker.feign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Test application.
 *
 * @author <a href="mailto:liaochuntao@live.com">liaochuntao</a>
 */
@SpringBootApplication
@EnableFeignClients
public class TestPolarisFeignApp {

	public static void main(String[] args) {
		SpringApplication.run(TestPolarisFeignApp.class);
	}

	@FeignClient(name = "feign-service-polaris",
			fallback = TestPolarisServiceFallback.class)
	public interface TestPolarisService {

		/**
		 * Get info of service B.
		 *
		 * @return info
		 */
		@GetMapping("/example/service/b/info")
		String info();

	}

	@Component
	public static class TestPolarisServiceFallback implements TestPolarisService {

		@Override
		public String info() {
			return "trigger the refuse";
		}

	}

}
