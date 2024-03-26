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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;

import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.rpc.enhancement.zuul.EnhancedPreZuulFilter;
import org.assertj.core.api.Assertions;
import org.assertj.core.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;
import static com.tencent.cloud.common.constant.MetadataConstant.HeaderName.CUSTOM_DISPOSABLE_METADATA;
import static com.tencent.cloud.common.constant.MetadataConstant.HeaderName.CUSTOM_METADATA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

/**
 * Test for {@link EncodeTransferMetadataZuulEnhancedPlugin}.
 *
 * @author quan, Shedfree Wu
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = RANDOM_PORT,
		classes = EncodeTransferMetadataZuulFilterTest.TestApplication.class,
		properties = {"spring.config.location = classpath:application-test.yml",
				"spring.main.web-application-type = reactive"})
public class EncodeTransferMetadataZuulFilterTest {

	private final MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
	@Autowired
	private ApplicationContext applicationContext;

	@BeforeEach
	void setUp() {
		RequestContext ctx = RequestContext.getCurrentContext();
		ctx.clear();
		ctx.setRequest(this.request);
	}

	@Test
	public void testRun() throws ZuulException, UnsupportedEncodingException {
		EnhancedPreZuulFilter filter = applicationContext.getBean(EnhancedPreZuulFilter.class);
		RequestContext context = RequestContext.getCurrentContext();
		context.set(SERVICE_ID_KEY, "test-service");

		MetadataContext metadataContext = MetadataContextHolder.get();
		metadataContext.setTransitiveMetadata(Maps.newHashMap("t-key", "t-value"));
		metadataContext.setDisposableMetadata(Maps.newHashMap("d-key", "d-value"));
		filter.run();

		final RequestContext ctx = RequestContext.getCurrentContext();
		Map<String, String> zuulRequestHeaders = ctx.getZuulRequestHeaders();
		// convert header to lower case in com.netflix.zuul.context.RequestContext.addZuulRequestHeader
		assertThat(zuulRequestHeaders.get(CUSTOM_METADATA.toLowerCase())).isNotNull();
		assertThat(zuulRequestHeaders.get(CUSTOM_DISPOSABLE_METADATA.toLowerCase())).isNotNull();

		String metadata = zuulRequestHeaders.get(CUSTOM_METADATA.toLowerCase());

		Assertions.assertThat(metadata).isNotNull();

		String decode = URLDecoder.decode(metadata, UTF_8);
		Map<String, String> transitiveMap = JacksonUtils.deserialize2Map(decode);
		// expect {"b":"2","t-key":"t-value"}
		Assertions.assertThat(transitiveMap.size()).isEqualTo(2);
		Assertions.assertThat(transitiveMap.get("b")).isEqualTo("2");
	}

	@SpringBootApplication
	protected static class TestApplication {

	}
}
