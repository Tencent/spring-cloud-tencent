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

package com.tencent.cloud.plugin.gateway.staining;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.plugin.gateway.staining.rule.RuleStainingExecutor;
import com.tencent.cloud.plugin.gateway.staining.rule.RuleStainingProperties;
import com.tencent.cloud.plugin.gateway.staining.rule.RuleTrafficStainer;
import com.tencent.cloud.plugin.gateway.staining.rule.StainingRuleManager;
import com.tencent.polaris.configuration.api.core.ConfigFile;
import com.tencent.polaris.configuration.api.core.ConfigFileService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link TrafficStainingGatewayFilter}.
 * @author lepdou 2022-07-12
 */
@ExtendWith(MockitoExtension.class)
public class TrafficStainingGatewayFilterTest {

	private final String testNamespace = "testNamespace";
	private final String testGroup = "testGroup";
	private final String testFileName = "rule.json";
	@Mock
	private GatewayFilterChain chain;
	@Mock
	private ServerWebExchange exchange;
	@Mock
	private ConfigFileService configFileService;

	@BeforeAll
	static void beforeAll() {
		Mockito.mockStatic(ApplicationContextAwareUtils.class);
		when(ApplicationContextAwareUtils
				.getProperties(any())).thenReturn("fooBar");
	}

	@Test
	public void testNoneTrafficStainingImplement() {
		TrafficStainingGatewayFilter filter = new TrafficStainingGatewayFilter(null);

		when(chain.filter(exchange)).thenReturn(Mono.empty());

		filter.filter(exchange, chain);

		verify(chain).filter(exchange);
	}

	@Test
	public void testMultiStaining() {
		TrafficStainer trafficStainer1 = Mockito.mock(TrafficStainer.class);
		TrafficStainer trafficStainer2 = Mockito.mock(TrafficStainer.class);

		when(trafficStainer1.getOrder()).thenReturn(1);
		when(trafficStainer2.getOrder()).thenReturn(2);

		Map<String, String> labels1 = new HashMap<>();
		labels1.put("k1", "v1");
		labels1.put("k2", "v2");
		when(trafficStainer1.apply(exchange)).thenReturn(labels1);

		Map<String, String> labels2 = new HashMap<>();
		labels2.put("k1", "v11");
		labels2.put("k3", "v3");
		when(trafficStainer2.apply(exchange)).thenReturn(labels2);

		TrafficStainingGatewayFilter filter = new TrafficStainingGatewayFilter(Arrays.asList(trafficStainer1, trafficStainer2));
		Map<String, String> result = filter.getStainedLabels(exchange);

		assertThat(result).isNotEmpty();
		assertThat(result.get("k1")).isEqualTo("v1");
		assertThat(result.get("k2")).isEqualTo("v2");
		assertThat(result.get("k3")).isEqualTo("v3");
	}

	@Test
	public void testNoTrafficStainers() {
		MetadataContext metadataContext = new MetadataContext();
		MetadataContextHolder.set(metadataContext);

		TrafficStainingGatewayFilter filter = new TrafficStainingGatewayFilter(null);
		filter.filter(exchange, chain);
		Map<String, String> map = metadataContext.getTransitiveMetadata();
		assertThat(map).isEmpty();
	}

	@Test
	public void testWithTrafficStainers() {
		MetadataContext metadataContext = new MetadataContext();
		MetadataContextHolder.set(metadataContext);

		RuleStainingProperties ruleStainingProperties = new RuleStainingProperties();
		ruleStainingProperties.setNamespace(testNamespace);
		ruleStainingProperties.setGroup(testGroup);
		ruleStainingProperties.setFileName(testFileName);

		ConfigFile configFile = Mockito.mock(ConfigFile.class);
		when(configFile.getContent()).thenReturn("{\n"
				+ "    \"rules\":[\n"
				+ "        {\n"
				+ "            \"conditions\":[\n"
				+ "                {\n"
				+ "                    \"key\":\"${http.query.uid}\",\n"
				+ "                    \"values\":[\"1000\"],\n"
				+ "                    \"operation\":\"EQUALS\"\n"
				+ "                }\n"
				+ "            ],\n"
				+ "            \"labels\":[\n"
				+ "                {\n"
				+ "                    \"key\":\"env\",\n"
				+ "                    \"value\":\"blue\"\n"
				+ "                }\n"
				+ "            ]\n"
				+ "        }\n"
				+ "    ]\n"
				+ "}");
		when(configFileService.getConfigFile(testNamespace, testGroup, testFileName)).thenReturn(configFile);

		StainingRuleManager stainingRuleManager = new StainingRuleManager(ruleStainingProperties, configFileService);
		RuleStainingExecutor ruleStainingExecutor = new RuleStainingExecutor();
		RuleTrafficStainer ruleTrafficStainer = new RuleTrafficStainer(stainingRuleManager, ruleStainingExecutor);

		TrafficStainingGatewayFilter filter = new TrafficStainingGatewayFilter(Collections.singletonList(ruleTrafficStainer));

		MockServerHttpRequest request = MockServerHttpRequest.get("/users")
				.queryParam("uid", "1000").build();
		MockServerWebExchange exchange = new MockServerWebExchange.Builder(request).build();

		filter.filter(exchange, chain);
		Map<String, String> map = metadataContext.getTransitiveMetadata();
		assertThat(map).isNotNull();
		assertThat(map.size()).isEqualTo(1);
		assertThat(map.get("env")).isEqualTo("blue");
	}
}
