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

package com.tencent.cloud.rpc.enhancement.plugin;

import java.io.UnsupportedEncodingException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;

import com.tencent.cloud.common.constant.HeaderConstant;
import com.tencent.cloud.common.constant.RouterConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.client.api.SDKContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;
import static com.tencent.polaris.test.common.Consts.NAMESPACE_TEST;
import static com.tencent.polaris.test.common.Consts.SERVICE_PROVIDER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Test For {@link PolarisEnhancedPluginUtils}.
 *
 * @author <a href="mailto:iskp.me@gmail.com">Elve.Xu</a> 2022/7/11
 */
@ExtendWith(MockitoExtension.class)
public class PolarisEnhancedPluginUtilsTest {

	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;
	private final RpcEnhancementReporterProperties reporterProperties = new RpcEnhancementReporterProperties();
	@Mock
	private SDKContext sdkContext;

	@BeforeAll
	static void beforeAll() {
		mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class);
		mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
				.thenReturn("unit-test");
		ApplicationContext applicationContext = mock(ApplicationContext.class);
		RpcEnhancementReporterProperties reporterProperties = mock(RpcEnhancementReporterProperties.class);
		doReturn(reporterProperties)
				.when(applicationContext).getBean(RpcEnhancementReporterProperties.class);
		mockedApplicationContextAwareUtils.when(ApplicationContextAwareUtils::getApplicationContext)
				.thenReturn(applicationContext);
	}

	@AfterAll
	static void afterAll() {
		mockedApplicationContextAwareUtils.close();
	}

	@BeforeEach
	void setUp() {
		MetadataContext.LOCAL_NAMESPACE = NAMESPACE_TEST;
		MetadataContext.LOCAL_SERVICE = SERVICE_PROVIDER;
	}

	@Test
	public void testServiceCallResult() throws URISyntaxException {

		ServiceCallResult serviceCallResult;

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.add(RouterConstant.ROUTER_LABEL_HEADER, "{\"k1\":\"v1\"}");

		assertThat(PolarisEnhancedPluginUtils.getLabelMap(requestHeaders)).isEqualTo(new HashMap<String, String>() {{
			put("k1", "v1");
		}});

		serviceCallResult = PolarisEnhancedPluginUtils.createServiceCallResult(
				"0.0.0.0",
				"test",
				null,
				null,
				new URI("http://0.0.0.0/"),
				requestHeaders,
				new HttpHeaders(),
				200,
				0,
				null
		);
		assertThat(serviceCallResult.getRetStatus()).isEqualTo(RetStatus.RetSuccess);

		serviceCallResult = PolarisEnhancedPluginUtils.createServiceCallResult(
				"0.0.0.0",
				"test",
				null,
				null,
				new URI("http://0.0.0.0/"),
				requestHeaders,
				new HttpHeaders(),
				502,
				0,
				null
		);
		assertThat(serviceCallResult.getRetStatus()).isEqualTo(RetStatus.RetFail);

		serviceCallResult = PolarisEnhancedPluginUtils.createServiceCallResult(
				"0.0.0.0",
				"test",
				null,
				null,
				new URI("http://0.0.0.0/"),
				requestHeaders,
				null,
				null,
				0,
				new SocketTimeoutException()
		);
		assertThat(serviceCallResult.getRetStatus()).isEqualTo(RetStatus.RetTimeout);

		serviceCallResult = PolarisEnhancedPluginUtils.createServiceCallResult(
				"0.0.0.0",
				"test",
				"0.0.0.0",
				8080,
				new URI("/"),
				requestHeaders,
				new HttpHeaders(),
				200,
				0,
				null
		);
		assertThat(serviceCallResult.getRetStatus()).isEqualTo(RetStatus.RetSuccess);
		assertThat(serviceCallResult.getHost()).isEqualTo("0.0.0.0");
		assertThat(serviceCallResult.getPort()).isEqualTo(8080);
	}

	@Test
	public void testResourceStat() throws URISyntaxException {

		ResourceStat resourceStat;

		resourceStat = PolarisEnhancedPluginUtils.createInstanceResourceStat("test",
				null,
				null,
				new URI("http://0.0.0.0/"),
				200,
				0,
				null
		);
		assertThat(resourceStat.getRetStatus()).isEqualTo(RetStatus.RetSuccess);

		resourceStat = PolarisEnhancedPluginUtils.createInstanceResourceStat("test",
				null,
				null,
				new URI("http://0.0.0.0/"),
				null,
				0,
				new SocketTimeoutException()
		);
		assertThat(resourceStat.getRetStatus()).isEqualTo(RetStatus.RetTimeout);

		resourceStat = PolarisEnhancedPluginUtils.createInstanceResourceStat("test",
				null,
				null,
				new URI("http://0.0.0.0/"),
				200,
				0,
				null
		);
		assertThat(resourceStat.getRetStatus()).isEqualTo(RetStatus.RetSuccess);
	}

	@Test
	public void testApplyWithDefaultConfig() {
		RpcEnhancementReporterProperties properties = new RpcEnhancementReporterProperties();

		ApplicationContext applicationContext = mock(ApplicationContext.class);
		doReturn(properties)
				.when(applicationContext).getBean(RpcEnhancementReporterProperties.class);
		mockedApplicationContextAwareUtils.when(ApplicationContextAwareUtils::getApplicationContext)
				.thenReturn(applicationContext);
		// Assert
		assertThat(PolarisEnhancedPluginUtils.apply(HttpStatus.OK)).isEqualTo(false);
		assertThat(PolarisEnhancedPluginUtils.apply(HttpStatus.INTERNAL_SERVER_ERROR)).isEqualTo(false);
		assertThat(PolarisEnhancedPluginUtils.apply(HttpStatus.BAD_GATEWAY)).isEqualTo(true);
	}

	@Test
	public void testApplyWithHttpStatus() {
		RpcEnhancementReporterProperties properties = new RpcEnhancementReporterProperties();
		properties.setStatuses(Arrays.asList(HttpStatus.BAD_GATEWAY, HttpStatus.INTERNAL_SERVER_ERROR));
		ApplicationContext applicationContext = mock(ApplicationContext.class);
		doReturn(properties)
				.when(applicationContext).getBean(RpcEnhancementReporterProperties.class);
		mockedApplicationContextAwareUtils.when(ApplicationContextAwareUtils::getApplicationContext)
				.thenReturn(applicationContext);
		// Assert
		assertThat(PolarisEnhancedPluginUtils.apply(null)).isEqualTo(false);
		assertThat(PolarisEnhancedPluginUtils.apply(HttpStatus.OK)).isEqualTo(false);
		assertThat(PolarisEnhancedPluginUtils.apply(HttpStatus.INTERNAL_SERVER_ERROR)).isEqualTo(true);
		assertThat(PolarisEnhancedPluginUtils.apply(HttpStatus.BAD_GATEWAY)).isEqualTo(true);
	}

	@Test
	public void testApplyWithoutIgnoreInternalServerError() {
		RpcEnhancementReporterProperties properties = new RpcEnhancementReporterProperties();
		// Mock Condition
		properties.getStatuses().clear();
		properties.setIgnoreInternalServerError(false);

		ApplicationContext applicationContext = mock(ApplicationContext.class);
		doReturn(properties)
				.when(applicationContext).getBean(RpcEnhancementReporterProperties.class);
		mockedApplicationContextAwareUtils.when(ApplicationContextAwareUtils::getApplicationContext)
				.thenReturn(applicationContext);

		// Assert
		assertThat(PolarisEnhancedPluginUtils.apply(HttpStatus.OK)).isEqualTo(false);
		assertThat(PolarisEnhancedPluginUtils.apply(HttpStatus.INTERNAL_SERVER_ERROR)).isEqualTo(true);
		assertThat(PolarisEnhancedPluginUtils.apply(HttpStatus.BAD_GATEWAY)).isEqualTo(true);
	}

	@Test
	public void testApplyWithIgnoreInternalServerError() {
		RpcEnhancementReporterProperties properties = new RpcEnhancementReporterProperties();
		// Mock Condition
		properties.getStatuses().clear();
		properties.setIgnoreInternalServerError(true);

		ApplicationContext applicationContext = mock(ApplicationContext.class);
		doReturn(properties)
				.when(applicationContext).getBean(RpcEnhancementReporterProperties.class);
		mockedApplicationContextAwareUtils.when(ApplicationContextAwareUtils::getApplicationContext)
				.thenReturn(applicationContext);

		// Assert
		assertThat(PolarisEnhancedPluginUtils.apply(HttpStatus.OK)).isEqualTo(false);
		assertThat(PolarisEnhancedPluginUtils.apply(HttpStatus.INTERNAL_SERVER_ERROR)).isEqualTo(false);
		assertThat(PolarisEnhancedPluginUtils.apply(HttpStatus.BAD_GATEWAY)).isEqualTo(true);
	}

	@Test
	public void testApplyWithoutSeries() {
		RpcEnhancementReporterProperties properties = new RpcEnhancementReporterProperties();
		// Mock Condition
		properties.getStatuses().clear();
		properties.getSeries().clear();

		ApplicationContext applicationContext = mock(ApplicationContext.class);
		doReturn(properties)
				.when(applicationContext).getBean(RpcEnhancementReporterProperties.class);
		mockedApplicationContextAwareUtils.when(ApplicationContextAwareUtils::getApplicationContext)
				.thenReturn(applicationContext);

		// Assert
		assertThat(PolarisEnhancedPluginUtils.apply(HttpStatus.OK)).isEqualTo(false);
		assertThat(PolarisEnhancedPluginUtils.apply(HttpStatus.INTERNAL_SERVER_ERROR)).isEqualTo(false);
		assertThat(PolarisEnhancedPluginUtils.apply(HttpStatus.BAD_GATEWAY)).isEqualTo(true);
	}

	@Test
	public void testApplyWithSeries() {
		RpcEnhancementReporterProperties properties = new RpcEnhancementReporterProperties();
		// Mock Condition
		properties.getStatuses().clear();
		properties.getSeries().clear();
		properties.getSeries().add(HttpStatus.Series.CLIENT_ERROR);

		ApplicationContext applicationContext = mock(ApplicationContext.class);
		doReturn(properties)
				.when(applicationContext).getBean(RpcEnhancementReporterProperties.class);
		mockedApplicationContextAwareUtils.when(ApplicationContextAwareUtils::getApplicationContext)
				.thenReturn(applicationContext);

		// Assert
		assertThat(PolarisEnhancedPluginUtils.apply(HttpStatus.OK)).isEqualTo(false);
		assertThat(PolarisEnhancedPluginUtils.apply(HttpStatus.INTERNAL_SERVER_ERROR)).isEqualTo(false);
		assertThat(PolarisEnhancedPluginUtils.apply(HttpStatus.BAD_GATEWAY)).isEqualTo(false);
		assertThat(PolarisEnhancedPluginUtils.apply(HttpStatus.FORBIDDEN)).isEqualTo(true);
	}


	@Test
	public void testGetRetStatusFromRequest() {

		HttpHeaders headers = new HttpHeaders();
		RetStatus ret = PolarisEnhancedPluginUtils.getRetStatusFromRequest(headers, RetStatus.RetFail);
		assertThat(ret).isEqualTo(RetStatus.RetFail);

		headers.set(HeaderConstant.INTERNAL_CALLEE_RET_STATUS, RetStatus.RetFlowControl.getDesc());
		ret = PolarisEnhancedPluginUtils.getRetStatusFromRequest(headers, RetStatus.RetFail);
		assertThat(ret).isEqualTo(RetStatus.RetFlowControl);

		headers.set(HeaderConstant.INTERNAL_CALLEE_RET_STATUS, RetStatus.RetReject.getDesc());
		ret = PolarisEnhancedPluginUtils.getRetStatusFromRequest(headers, RetStatus.RetFail);
		assertThat(ret).isEqualTo(RetStatus.RetReject);
	}

	@Test
	public void testGetActiveRuleNameFromRequest() throws UnsupportedEncodingException {

		HttpHeaders headers = new HttpHeaders();
		String ruleName = PolarisEnhancedPluginUtils.getActiveRuleNameFromRequest(headers);
		assertThat(ruleName).isEqualTo("");

		String encodedRuleName = URLEncoder.encode("mock_rule", UTF_8);
		headers.set(HeaderConstant.INTERNAL_ACTIVE_RULE_NAME, encodedRuleName);
		ruleName = PolarisEnhancedPluginUtils.getActiveRuleNameFromRequest(headers);
		assertThat(ruleName).isEqualTo("mock_rule");
	}

}
