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

package com.tencent.cloud.metadata.core.intercepter;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.metadata.config.MetadataLocalProperties;
import com.tencent.cloud.metadata.core.EncodeTransferMedataFeignInterceptor;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

/**
 * Test for {@link EncodeTransferMedataFeignInterceptor}.
 *
 * @author Haotian Zhang
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = DEFINED_PORT,
		classes = EncodeTransferMedataFeignInterceptorTest.TestApplication.class,
		properties = { "server.port=8081",
				"spring.config.location = classpath:application-test.yml" })
public class EncodeTransferMedataFeignInterceptorTest {

	@Autowired
	private MetadataLocalProperties metadataLocalProperties;

	@Autowired
	private TestApplication.TestFeign testFeign;

	@Test
	public void test1() {
		String metadata = testFeign.test();
		Assertions.assertThat(metadata)
				.isEqualTo("{\"a\":\"11\",\"b\":\"22\",\"c\":\"33\"}");
		Assertions.assertThat(metadataLocalProperties.getContent().get("a"))
				.isEqualTo("1");
		Assertions.assertThat(metadataLocalProperties.getContent().get("b"))
				.isEqualTo("2");
		Assertions
				.assertThat(MetadataContextHolder.get().getTransitiveCustomMetadata("a"))
				.isEqualTo("11");
		Assertions
				.assertThat(MetadataContextHolder.get().getTransitiveCustomMetadata("b"))
				.isEqualTo("22");
		Assertions
				.assertThat(MetadataContextHolder.get().getTransitiveCustomMetadata("c"))
				.isEqualTo("33");
	}

	@SpringBootApplication
	@EnableFeignClients
	@RestController
	protected static class TestApplication {

		@RequestMapping("/test")
		public String test(
				@RequestHeader(MetadataConstant.HeaderName.CUSTOM_METADATA) String customMetadataStr)
				throws UnsupportedEncodingException {
			return URLDecoder.decode(customMetadataStr, "UTF-8");
		}

		@FeignClient(name = "test-feign", url = "http://localhost:8081")
		public interface TestFeign {

			@RequestMapping(value = "/test",
					headers = { MetadataConstant.HeaderName.CUSTOM_METADATA
							+ "={\"a\":\"11" + "\",\"b\":\"22\",\"c\":\"33\"}" })
			String test();

		}

		@Configuration
		static class TestRequestInterceptor implements RequestInterceptor {

			@Override
			public void apply(RequestTemplate template) {
				template.header(MetadataConstant.HeaderName.CUSTOM_METADATA,
						"{\"a\":\"11\",\"b\":\"22\",\"c\":\"33\"}");
			}

		}

	}

}
