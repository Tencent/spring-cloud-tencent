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

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.tencent.cloud.common.constant.MetadataConstant;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.metadata.config.MetadataLocalProperties;
import com.tencent.cloud.common.tsf.TsfContextUtils;
import com.tencent.cloud.rpc.enhancement.plugin.EnhancedPluginContext;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test TSF header encode when Consul is disabled.
 */
public class EncodeTransferTsfHeaderCompatibleTest {

	@BeforeEach
	@AfterEach
	public void reset() throws Exception {
		MetadataContextHolder.remove();
		Field isTsfConsulEnabledFirst = TsfContextUtils.class.getDeclaredField("isTsfConsulEnabledFirstConfiguration");
		isTsfConsulEnabledFirst.setAccessible(true);
		((AtomicBoolean) isTsfConsulEnabledFirst.get(null)).set(true);

		Field tsfConsulEnabledField = TsfContextUtils.class.getDeclaredField("tsfConsulEnabled");
		tsfConsulEnabledField.setAccessible(true);
		tsfConsulEnabledField.set(null, false);
	}

	@Test
	public void testEncodeTsfHeaderWhenCompatibleWithoutConsul() throws Throwable {
		MetadataContextHolder.get().setTransitiveMetadata(Collections.singletonMap("feat", "test"));
		MetadataLocalProperties properties = new MetadataLocalProperties();
		properties.setTsfHeaderCompatible(true);
		EncodeTransferMedataFeignEnhancedPlugin plugin =
				new EncodeTransferMedataFeignEnhancedPlugin(new ArrayList<>(), properties);

		Request request = createRequest();
		EnhancedPluginContext context = new EnhancedPluginContext();
		context.setOriginRequest(request);
		plugin.run(context);

		assertThat(request.headers()).containsKey(MetadataConstant.HeaderName.TSF_TAGS);
	}

	@Test
	public void testSkipTsfHeaderWhenIncompatibleWithoutConsul() throws Throwable {
		MetadataContextHolder.get().setTransitiveMetadata(Collections.singletonMap("feat", "test"));
		MetadataLocalProperties properties = new MetadataLocalProperties();
		properties.setTsfHeaderCompatible(false);
		EncodeTransferMedataFeignEnhancedPlugin plugin =
				new EncodeTransferMedataFeignEnhancedPlugin(new ArrayList<>(), properties);

		Request request = createRequest();
		EnhancedPluginContext context = new EnhancedPluginContext();
		context.setOriginRequest(request);
		plugin.run(context);

		assertThat(request.headers()).doesNotContainKey(MetadataConstant.HeaderName.TSF_TAGS);
		assertThat(request.headers()).containsKey(MetadataConstant.HeaderName.CUSTOM_METADATA);
	}

	@Test
	public void testEncodeTsfHeaderWhenConsulEnabled() throws Throwable {
		Field tsfConsulEnabledField = TsfContextUtils.class.getDeclaredField("tsfConsulEnabled");
		tsfConsulEnabledField.setAccessible(true);
		tsfConsulEnabledField.set(null, true);

		MetadataContextHolder.get().setTransitiveMetadata(Collections.singletonMap("feat", "test"));
		EncodeTransferMedataFeignEnhancedPlugin plugin =
				new EncodeTransferMedataFeignEnhancedPlugin(new ArrayList<>(), new MetadataLocalProperties());

		Request request = createRequest();
		EnhancedPluginContext context = new EnhancedPluginContext();
		context.setOriginRequest(request);
		plugin.run(context);

		assertThat(request.headers()).containsKey(MetadataConstant.HeaderName.TSF_TAGS);
	}

	private Request createRequest() {
		Map<String, Collection<String>> headers = Collections.emptyMap();
		return Request.create(Request.HttpMethod.GET, "http://localhost/test",
				headers, new byte[0], StandardCharsets.UTF_8, new RequestTemplate());
	}
}
