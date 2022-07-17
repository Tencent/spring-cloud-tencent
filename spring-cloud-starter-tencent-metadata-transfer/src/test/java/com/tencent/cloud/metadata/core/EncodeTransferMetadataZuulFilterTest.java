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
import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.util.JacksonUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.test.context.junit4.SpringRunner;

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Test for {@link EncodeTransferMetadataZuulFilter}.
 *
 * @author quan
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT,
		classes = EncodeTransferMetadataZuulFilterTest.TestApplication.class,
		properties = {"spring.config.location = classpath:application-test.yml",
				"spring.main.web-application-type = reactive"})
public class EncodeTransferMetadataZuulFilterTest {

	private final MockMultipartHttpServletRequest request = new MockMultipartHttpServletRequest();
	@Autowired
	private ApplicationContext applicationContext;

	@Before
	public void init() {
		RequestContext ctx = RequestContext.getCurrentContext();
		ctx.clear();
		ctx.setRequest(this.request);
	}

	@Test
	public void multiplePartNamesWithMultipleParts() throws UnsupportedEncodingException {
		EncodeTransferMetadataZuulFilter filter = applicationContext.getBean(EncodeTransferMetadataZuulFilter.class);
		filter.run();
		final RequestContext ctx = RequestContext.getCurrentContext();
		Map<String, String> zuulRequestHeaders = ctx.getZuulRequestHeaders();
		String metadata = zuulRequestHeaders.get(MetadataConstant.HeaderName.CUSTOM_METADATA.toLowerCase());
		Assertions.assertThat(metadata).isNotNull();

		String decode = URLDecoder.decode(metadata, UTF_8);
		Map<String, String> transitiveMap = JacksonUtils.deserialize2Map(decode);
		Assertions.assertThat(transitiveMap.size()).isEqualTo(1);
		Assertions.assertThat(transitiveMap.get("b")).isEqualTo("2");
	}

	@SpringBootApplication
	protected static class TestApplication {

	}
}
