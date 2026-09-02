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

package com.tencent.cloud.metadata.core;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.UrlUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
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
 * Test TSF header decode when Consul is disabled and tsf-header-compatible is true.
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT,
		classes = DecodeTransferTsfHeaderCompatibleTest.TestApplication.class,
		properties = {"spring.config.location = classpath:application-test.yml",
				"spring.main.web-application-type = servlet",
				"spring.cloud.gateway.enabled = false",
				"spring.cloud.tencent.metadata.tsf-header-compatible = true"})
public class DecodeTransferTsfHeaderCompatibleTest {

	@Autowired
	private DecodeTransferMetadataServletFilter metadataServletFilter;

	@Test
	public void testDecodeTsfTagsWithoutConsul() throws ServletException, IOException {
		AtomicReference<String> featValue = new AtomicReference<>();
		FilterChain filterChain = (servletRequest, servletResponse) ->
				featValue.set(MetadataContextHolder.get().getContext(MetadataContext.FRAGMENT_TRANSITIVE, "feat"));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(MetadataConstant.HeaderName.TSF_TAGS,
				UrlUtils.encode("[{\"k\":\"feat\",\"v\":\"test\",\"f\":[\"0\"]}]"));
		metadataServletFilter.doFilter(request, new MockHttpServletResponse(), filterChain);

		assertThat(featValue.get()).isEqualTo("test");
	}

	@SpringBootApplication
	protected static class TestApplication {

	}
}
