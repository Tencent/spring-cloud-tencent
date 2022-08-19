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

package com.tencent.cloud.polaris.router.zuul;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.IClientConfig;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.niws.client.http.RestClient;
import com.netflix.zuul.context.RequestContext;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.polaris.loadbalancer.PolarisLoadBalancer;
import com.tencent.cloud.polaris.router.PolarisRouterContext;
import com.tencent.cloud.polaris.router.RouterRuleLabelResolver;
import com.tencent.cloud.polaris.router.spi.ServletRouterLabelResolver;
import okhttp3.OkHttpClient;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.cloud.netflix.ribbon.apache.RibbonLoadBalancingHttpClient;
import org.springframework.cloud.netflix.ribbon.okhttp.OkHttpLoadBalancingClient;
import org.springframework.cloud.netflix.ribbon.support.RibbonCommandContext;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.FallbackProvider;
import org.springframework.cloud.netflix.zuul.filters.route.RestClientRibbonCommand;
import org.springframework.cloud.netflix.zuul.filters.route.RibbonCommandFactory;
import org.springframework.cloud.netflix.zuul.filters.route.apache.HttpClientRibbonCommand;
import org.springframework.cloud.netflix.zuul.filters.route.okhttp.OkHttpRibbonCommand;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.LOAD_BALANCER_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.RETRYABLE_KEY;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

/**
 * test for {@link PolarisRibbonRoutingFilter}.
 *
 * @author jarvisxiong 2022-08-09
 */
@RunWith(MockitoJUnitRunner.class)
public class PolarisRibbonRoutingFilterTest {

	private static final String callerService = "callerService";
	private static final String calleeService = "calleeService";
	private static MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils;
	private static MockedStatic<MetadataContextHolder> mockedMetadataContextHolder;
	@Mock
	private StaticMetadataManager staticMetadataManager;
	@Mock
	private RouterRuleLabelResolver routerRuleLabelResolver;
	@Mock
	private ServletRouterLabelResolver routerLabelResolver;
	@Mock
	private ProxyRequestHelper proxyRequestHelper;
	@Mock
	private RibbonCommandFactory<?> ribbonCommandFactory;
	@Mock
	private FallbackProvider fallbackProvider;
	@Mock
	private PolarisLoadBalancer polarisLoadBalancer;

	@BeforeClass
	public static void beforeClass() {
		mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class);
		mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
				.thenReturn(callerService);

		MetadataContext metadataContext = Mockito.mock(MetadataContext.class);

		// mock transitive metadata
		Map<String, String> transitiveLabels = new HashMap<>();
		transitiveLabels.put("t1", "v1");
		transitiveLabels.put("t2", "v2");
		when(metadataContext.getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE)).thenReturn(transitiveLabels);

		mockedMetadataContextHolder = Mockito.mockStatic(MetadataContextHolder.class);
		mockedMetadataContextHolder.when(MetadataContextHolder::get).thenReturn(metadataContext);
	}

	@AfterClass
	public static void afterClass() {
		mockedApplicationContextAwareUtils.close();
		mockedMetadataContextHolder.close();
	}

	@Test
	public void testGenRouterContext() {
		PolarisRibbonRoutingFilter polarisRibbonRoutingFilter = new PolarisRibbonRoutingFilter(proxyRequestHelper,
				ribbonCommandFactory, staticMetadataManager, routerRuleLabelResolver,
				Lists.newArrayList(routerLabelResolver));

		Map<String, String> localMetadata = new HashMap<>();
		localMetadata.put("env", "blue");
		when(staticMetadataManager.getMergedStaticMetadata()).thenReturn(localMetadata);

		Set<String> expressionLabelKeys = Sets.newHashSet("${http.header.k1}", "${http.query.userid}");
		when(routerRuleLabelResolver.getExpressionLabelKeys(anyString(), anyString(), anyString()))
				.thenReturn(expressionLabelKeys);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("/" + calleeService + "/users");
		request.addHeader("k1", "v1");
		request.setQueryString("userid=zhangsan");

		Map<String, String> customMetadata = new HashMap<>();
		customMetadata.put("k2", "v2");
		when(routerLabelResolver.resolve(request, expressionLabelKeys)).thenReturn(customMetadata);

		PolarisRouterContext routerContext = polarisRibbonRoutingFilter.genRouterContext(request, calleeService);

		Map<String, String> routerLabels = routerContext.getLabels(PolarisRouterContext.ROUTER_LABELS);
		Assert.assertEquals("v1", routerLabels.get("${http.header.k1}"));
		Assert.assertEquals("zhangsan", routerLabels.get("${http.query.userid}"));
		Assert.assertEquals("blue", routerLabels.get("env"));
		Assert.assertEquals("v1", routerLabels.get("t1"));
		Assert.assertEquals("v2", routerLabels.get("t2"));
	}

	@Test
	public void testHttpCallWithoutRouter() {
		ZuulProperties zuulProperties = new ZuulProperties();
		zuulProperties.setRibbonIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD);
		zuulProperties.setThreadPool(new ZuulProperties.HystrixThreadPool());

		PolarisRibbonRoutingFilter polarisRibbonRoutingFilter = new PolarisRibbonRoutingFilter(
				new ProxyRequestHelper(zuulProperties), ribbonCommandFactory, staticMetadataManager,
				routerRuleLabelResolver, Lists.newArrayList(routerLabelResolver));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("http://" + calleeService + "/users");
		request.addHeader("k1", "v1");
		request.setQueryString("userid=zhangsan");

		RequestContext context = new RequestContext();
		context.setRequest(request);
		context.set(SERVICE_ID_KEY, calleeService);
		context.set(RETRYABLE_KEY, Boolean.FALSE);
		context.set(LOAD_BALANCER_KEY, calleeService);
		RequestContext.testSetCurrentContext(context);
		RibbonCommandContext commandContext = polarisRibbonRoutingFilter.buildCommandContext(context);

		IClientConfig clientConfig = IClientConfig.Builder.newBuilder().build();
		RibbonLoadBalancingHttpClient client = new RibbonLoadBalancingHttpClient(clientConfig, null);
		client.setLoadBalancer(polarisLoadBalancer);

		HttpClientRibbonCommand command = new HttpClientRibbonCommand(calleeService, client, commandContext,
				zuulProperties, fallbackProvider, clientConfig);
		command.execute();

		verify(polarisLoadBalancer).chooseServer(calleeService);
		verify(staticMetadataManager, times(0)).getMergedStaticMetadata();
	}

	@Test
	public void testRestCallWithoutRouter() {
		ZuulProperties zuulProperties = new ZuulProperties();
		zuulProperties.setRibbonIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD);
		zuulProperties.setThreadPool(new ZuulProperties.HystrixThreadPool());

		PolarisRibbonRoutingFilter polarisRibbonRoutingFilter = new PolarisRibbonRoutingFilter(
				new ProxyRequestHelper(zuulProperties), ribbonCommandFactory, staticMetadataManager,
				routerRuleLabelResolver, Lists.newArrayList(routerLabelResolver));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("http://" + calleeService + "/users");
		request.addHeader("k1", "v1");
		request.setQueryString("userid=zhangsan");
		request.setMethod("GET");

		RequestContext context = new RequestContext();
		context.setRequest(request);
		context.set(SERVICE_ID_KEY, calleeService);
		context.set(RETRYABLE_KEY, Boolean.FALSE);
		context.set(LOAD_BALANCER_KEY, calleeService);
		RequestContext.testSetCurrentContext(context);
		RibbonCommandContext commandContext = polarisRibbonRoutingFilter.buildCommandContext(context);

		IClientConfig clientConfig = IClientConfig.Builder.newBuilder().build();
		RestClient restClient = new RestClient(polarisLoadBalancer);

		RestClientRibbonCommand command = new RestClientRibbonCommand(calleeService, restClient, commandContext,
				zuulProperties, fallbackProvider, clientConfig);
		command.execute();

		// RestClient not use loadBalancerKey
		verify(polarisLoadBalancer).chooseServer(null);
		verify(staticMetadataManager, times(0)).getMergedStaticMetadata();
	}

	@Test
	public void testOkHttpCallWithoutRouter() {
		ZuulProperties zuulProperties = new ZuulProperties();
		zuulProperties.setRibbonIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD);
		zuulProperties.setThreadPool(new ZuulProperties.HystrixThreadPool());

		PolarisRibbonRoutingFilter polarisRibbonRoutingFilter = new PolarisRibbonRoutingFilter(
				new ProxyRequestHelper(zuulProperties), ribbonCommandFactory, staticMetadataManager,
				routerRuleLabelResolver, Lists.newArrayList(routerLabelResolver));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("http://" + calleeService + "/users");
		request.addHeader("k1", "v1");
		request.setQueryString("userid=zhangsan");

		RequestContext context = new RequestContext();
		context.setRequest(request);
		context.set(SERVICE_ID_KEY, calleeService);
		context.set(RETRYABLE_KEY, Boolean.FALSE);
		context.set(LOAD_BALANCER_KEY, calleeService);
		RequestContext.testSetCurrentContext(context);
		RibbonCommandContext commandContext = polarisRibbonRoutingFilter.buildCommandContext(context);

		IClientConfig clientConfig = IClientConfig.Builder.newBuilder().build();
		OkHttpLoadBalancingClient client = new OkHttpLoadBalancingClient(new OkHttpClient(), clientConfig, null);
		client.setLoadBalancer(polarisLoadBalancer);

		OkHttpRibbonCommand command = new OkHttpRibbonCommand(calleeService, client, commandContext, zuulProperties,
				fallbackProvider, clientConfig);
		command.execute();

		verify(polarisLoadBalancer).chooseServer(calleeService);
		verify(staticMetadataManager, times(0)).getMergedStaticMetadata();
	}

	@Test
	public void testHttpCallWithRouter() {
		ZuulProperties zuulProperties = new ZuulProperties();
		zuulProperties.setRibbonIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD);
		zuulProperties.setThreadPool(new ZuulProperties.HystrixThreadPool());

		PolarisRibbonRoutingFilter polarisRibbonRoutingFilter = new PolarisRibbonRoutingFilter(
				new ProxyRequestHelper(zuulProperties), ribbonCommandFactory, staticMetadataManager,
				routerRuleLabelResolver, Lists.newArrayList(routerLabelResolver));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("http://" + calleeService + "/users");
		request.addHeader("k1", "v1");
		request.setQueryString("userid=zhangsan");

		RequestContext context = new RequestContext();
		context.setRequest(request);
		context.set(SERVICE_ID_KEY, calleeService);
		context.set(RETRYABLE_KEY, Boolean.FALSE);
		PolarisRouterContext routerContext = polarisRibbonRoutingFilter.genRouterContext(request, calleeService);
		context.set(LOAD_BALANCER_KEY, routerContext);
		RequestContext.testSetCurrentContext(context);
		RibbonCommandContext commandContext = polarisRibbonRoutingFilter.buildCommandContext(context);

		IClientConfig clientConfig = IClientConfig.Builder.newBuilder().build();
		RibbonLoadBalancingHttpClient client = new RibbonLoadBalancingHttpClient(clientConfig, null);
		client.setLoadBalancer(polarisLoadBalancer);

		HttpClientRibbonCommand command = new HttpClientRibbonCommand(calleeService, client, commandContext,
				zuulProperties, fallbackProvider, clientConfig);
		command.execute();
		verify(polarisLoadBalancer).chooseServer(routerContext);
		verify(staticMetadataManager, times(1)).getMergedStaticMetadata();
	}

	@Test
	public void testRestCallWithRouter() {
		ZuulProperties zuulProperties = new ZuulProperties();
		zuulProperties.setRibbonIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD);
		zuulProperties.setThreadPool(new ZuulProperties.HystrixThreadPool());

		PolarisRibbonRoutingFilter polarisRibbonRoutingFilter = new PolarisRibbonRoutingFilter(
				new ProxyRequestHelper(zuulProperties), ribbonCommandFactory, staticMetadataManager,
				routerRuleLabelResolver, Lists.newArrayList(routerLabelResolver));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("http://" + calleeService + "/users");
		request.addHeader("k1", "v1");
		request.setQueryString("userid=zhangsan");
		request.setMethod("GET");

		RequestContext context = new RequestContext();
		context.setRequest(request);
		context.set(SERVICE_ID_KEY, calleeService);
		context.set(RETRYABLE_KEY, Boolean.FALSE);
		PolarisRouterContext routerContext = polarisRibbonRoutingFilter.genRouterContext(request, calleeService);
		context.set(LOAD_BALANCER_KEY, routerContext);
		RequestContext.testSetCurrentContext(context);
		RibbonCommandContext commandContext = polarisRibbonRoutingFilter.buildCommandContext(context);

		IClientConfig clientConfig = IClientConfig.Builder.newBuilder().build();
		RestClient restClient = new RestClient(polarisLoadBalancer);

		RestClientRibbonCommand command = new RestClientRibbonCommand(calleeService, restClient, commandContext,
				zuulProperties, fallbackProvider, clientConfig);
		command.execute();

		// RestClient not use loadBalancerKey
		verify(polarisLoadBalancer).chooseServer(null);
		verify(staticMetadataManager, times(1)).getMergedStaticMetadata();
	}

	@Test
	public void testOkHttpCallWithRouter() {
		ZuulProperties zuulProperties = new ZuulProperties();
		zuulProperties.setRibbonIsolationStrategy(HystrixCommandProperties.ExecutionIsolationStrategy.THREAD);
		zuulProperties.setThreadPool(new ZuulProperties.HystrixThreadPool());

		PolarisRibbonRoutingFilter polarisRibbonRoutingFilter = new PolarisRibbonRoutingFilter(
				new ProxyRequestHelper(zuulProperties), ribbonCommandFactory, staticMetadataManager,
				routerRuleLabelResolver, Lists.newArrayList(routerLabelResolver));

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setRequestURI("http://" + calleeService + "/users");
		request.addHeader("k1", "v1");
		request.setQueryString("userid=zhangsan");
		request.setMethod("GET");

		RequestContext context = new RequestContext();
		context.setRequest(request);
		context.set(SERVICE_ID_KEY, calleeService);
		context.set(RETRYABLE_KEY, Boolean.FALSE);
		PolarisRouterContext routerContext = polarisRibbonRoutingFilter.genRouterContext(request, calleeService);
		context.set(LOAD_BALANCER_KEY, routerContext);
		RequestContext.testSetCurrentContext(context);
		RibbonCommandContext commandContext = polarisRibbonRoutingFilter.buildCommandContext(context);

		IClientConfig clientConfig = IClientConfig.Builder.newBuilder().build();
		// Retry once by default, so close retry
		clientConfig.set(CommonClientConfigKey.MaxAutoRetriesNextServer, 0);

		OkHttpLoadBalancingClient client = new OkHttpLoadBalancingClient(new OkHttpClient(), clientConfig, null);
		client.setLoadBalancer(polarisLoadBalancer);

		OkHttpRibbonCommand command = new OkHttpRibbonCommand(calleeService, client, commandContext, zuulProperties,
				fallbackProvider, clientConfig);
		command.execute();

		verify(polarisLoadBalancer).chooseServer(routerContext);
		verify(staticMetadataManager, times(1)).getMergedStaticMetadata();
	}
}
