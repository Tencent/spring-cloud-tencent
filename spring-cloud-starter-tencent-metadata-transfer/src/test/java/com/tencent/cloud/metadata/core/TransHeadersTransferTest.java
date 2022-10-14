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

package com.tencent.cloud.metadata.core;

import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.metadata.support.DynamicEnvironmentVariable;
import com.tencent.cloud.metadata.support.DynamicEnvironmentVariablesSpringJUnit4ClassRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.WebHandler;
import reactor.core.publisher.Flux;

import java.util.Map;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test for {@link TransHeadersTransfer}.
 *
 * @author lingxiao.wlx
 */
@RunWith(DynamicEnvironmentVariablesSpringJUnit4ClassRunner.class)
@DynamicEnvironmentVariable(name = "SCT_TRAFFIC_CONTENT_RAW_TRANSHEADERS", value = "header1,header2,header3")
@SpringBootTest(webEnvironment = DEFINED_PORT,
		classes = TransHeadersTransferTest.TestApplication.class,
		properties = {"server.port=8081", "spring.config.location = classpath:application-test.yml"})
public class TransHeadersTransferTest {

	@Autowired
	private WebApplicationContext webContext;

	@Autowired
	private DecodeTransferMetadataServletFilter metadataServletFilter;

	private MockMvc mockMvc;

	private WebTestClient webTestClient;

	@Before
	public void setupMockMvc() throws Exception {
		mockMvc = MockMvcBuilders.webAppContextSetup(webContext).addFilter(metadataServletFilter).build();
		webTestClient = WebTestClient.bindToApplicationContext(webContext).build();
	}

	@Test
	public void transferTest() throws Exception {
		MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/transHeaders")
				.header("header1", "1")
				.header("header2", "2")
				.header("header3", "3")
				.characterEncoding("UTF-8"))
				.andExpect(status().isOk())
				.andDo(MockMvcResultHandlers.print(System.err))
				.andReturn();

		MockHttpServletResponse response = mvcResult.getResponse();
		String contentAsString = response.getContentAsString();
		Map<String, String> map = JacksonUtils.deserialize2Map(contentAsString);
		System.out.println(contentAsString);
	}

	@Test
	public void transferReactiveTest(){
		FluxExchangeResult<Map<String, String>> result = webTestClient.get().uri("/transHeaders")
				.header("header1", "1")
				.header("header2", "2")
				.header("header3", "3")
				.accept(MediaType.APPLICATION_JSON)
				.exchange()
				.expectStatus().isOk()
				.returnResult(new ParameterizedTypeReference<Map<String, String>>() {
				});

		Flux<Map<String, String>> responseBody = result.getResponseBody();


	}

	@SpringBootApplication
	@EnableFeignClients
	@RestController
	protected static class TestApplication {
		@GetMapping("/transHeaders")
		public Map<String, String> test() {
			return MetadataContextHolder.get().getTransHeadersKV();
		}
	}
}
