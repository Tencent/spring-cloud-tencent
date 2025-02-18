/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.polaris.router.instrument.scgmvc;

import java.io.IOException;

import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.polaris.metadata.core.MessageMetadataContainer;
import com.tencent.polaris.metadata.core.MetadataContainer;
import com.tencent.polaris.metadata.core.MetadataType;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockCookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.springframework.cloud.gateway.server.mvc.filter.FormFilter.FORM_FILTER_ORDER;

/**
 * Test for ${@link RouterLabelMvcFilter}.
 *
 * @author luguoji
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = RouterLabelMvcFilterTest.TestApplication.class,
		properties = {"spring.cloud.polaris.namespace=test", "spring.application.name=test", "spring.main.web-application-type=servlet", "spring.cloud.gateway.enabled=false"})
public class RouterLabelMvcFilterTest {

	@Test
	public void testRouterLabelMvc() {
		try {
			RouterLabelMvcFilter routerLabelMvcFilter = new RouterLabelMvcFilter();

			assertThat(routerLabelMvcFilter.getOrder())
					.isEqualTo(FORM_FILTER_ORDER - 1);

			FilterChain filterChain = (servletRequest, servletResponse) -> {

			};

			MockHttpServletRequest request = new MockHttpServletRequest();
			request.addHeader("uid", "1000");
			request.setCookies(new MockCookie("k1", "v1"));
			request.setMethod(HttpMethod.POST.name());
			request.setRequestURI("/test/path");
			request.setQueryString("q1=a1");

			MockHttpServletResponse response = new MockHttpServletResponse();
			routerLabelMvcFilter.doFilter(request, response, filterChain);
			// get message metadata container
			MetadataContainer metadataContainer = MetadataContextHolder.get()
					.getMetadataContainer(MetadataType.MESSAGE, false);
			// method
			assertThat(metadataContainer.getRawMetadataStringValue(MessageMetadataContainer.LABEL_KEY_METHOD)).isEqualTo(HttpMethod.POST.toString());
			// path
			assertThat(metadataContainer.getRawMetadataStringValue(MessageMetadataContainer.LABEL_KEY_PATH)).isEqualTo("/test/path");
			// header
			assertThat(metadataContainer.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_HEADER, "uid")).isEqualTo("1000");
			// cookie
			assertThat(metadataContainer.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_COOKIE, "k1")).isEqualTo("v1");
			// query
			assertThat(metadataContainer.getRawMetadataMapValue(MessageMetadataContainer.LABEL_MAP_KEY_QUERY, "q1")).isEqualTo("a1");
		}
		catch (ServletException | IOException e) {
			fail("Exception encountered.", e);
		}
	}

	@SpringBootApplication
	protected static class TestApplication {

	}
}
