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

package com.tencent.cloud.metadata.core;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.config.MetadataLocalProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Test for  {@link DecodeTransferMetadataServletFilter}.
 *
 * @author Haotian Zhang
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT,
		classes = DecodeTransferMetadataServletFilterTest.TestApplication.class,
		properties = {"spring.config.location = classpath:application-test.yml"})
public class DecodeTransferMetadataServletFilterTest {

	@Autowired
	private MetadataLocalProperties metadataLocalProperties;

	@Autowired
	private DecodeTransferMetadataServletFilter metadataServletFilter;

	@Test
	public void test1() throws ServletException, IOException {
		// Create mock FilterChain
		FilterChain filterChain = (servletRequest, servletResponse) -> {

		};

		// Mock request
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(MetadataConstant.HeaderName.CUSTOM_METADATA, "{\"c\": \"3\"}");
		MockHttpServletResponse response = new MockHttpServletResponse();
		metadataServletFilter.doFilter(request, response, filterChain);
		assertThat(metadataLocalProperties.getContent().get("a")).isEqualTo("1");
		assertThat(metadataLocalProperties.getContent().get("b")).isEqualTo("2");
		assertThat(metadataLocalProperties.getContent().get("c")).isNull();
	}

	@SpringBootApplication
	protected static class TestApplication {

	}
}
