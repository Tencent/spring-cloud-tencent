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

package com.tencent.cloud.polaris.router.resttemplate;

import java.io.IOException;
import java.net.URI;

import com.tencent.cloud.polaris.router.PolarisRouterContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.ClientHttpResponseStatusCodeException;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRecoveryCallback;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryContext;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryPolicy;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRequestFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerRetryProperties;
import org.springframework.cloud.client.loadbalancer.RetryLoadBalancerInterceptor;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.retry.RetryListener;
import org.springframework.retry.backoff.BackOffPolicy;
import org.springframework.retry.backoff.NoBackOffPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;

/**
 * Override {@link RetryLoadBalancerInterceptor} for passing router context.
 *
 * @author lepdou 2022-10-09
 */
public class PolarisRetryLoadBalancerInterceptor extends RetryLoadBalancerInterceptor {

	private static final Log LOG = LogFactory.getLog(PolarisRetryLoadBalancerInterceptor.class);

	private final LoadBalancerClient loadBalancer;

	private final LoadBalancerRetryProperties lbProperties;

	private final LoadBalancerRequestFactory requestFactory;

	private final LoadBalancedRetryFactory lbRetryFactory;

	private final RouterContextFactory routerContextFactory;

	public PolarisRetryLoadBalancerInterceptor(LoadBalancerClient loadBalancer, LoadBalancerRetryProperties lbProperties,
			LoadBalancerRequestFactory requestFactory, LoadBalancedRetryFactory lbRetryFactory, RouterContextFactory routerContextFactory) {
		super(loadBalancer, lbProperties, requestFactory, lbRetryFactory);
		this.loadBalancer = loadBalancer;
		this.lbProperties = lbProperties;
		this.requestFactory = requestFactory;
		this.lbRetryFactory = lbRetryFactory;
		this.routerContextFactory = routerContextFactory;
	}

	@Override
	public ClientHttpResponse intercept(final HttpRequest request, final byte[] body,
			final ClientHttpRequestExecution execution) throws IOException {

		final URI originalUri = request.getURI();
		final String serviceName = originalUri.getHost();
		Assert.state(serviceName != null, "Request URI does not contain a valid hostname: " + originalUri);

		final LoadBalancedRetryPolicy retryPolicy = lbRetryFactory.createRetryPolicy(serviceName, loadBalancer);

		//1. create router context
		PolarisRouterContext routerContext = routerContextFactory.create(request, body, serviceName);

		RetryTemplate template = createRetryTemplate(serviceName, request, routerContext, retryPolicy);

		//2. do loadbalancer is template.execute
		return template.execute(context -> {
			ServiceInstance serviceInstance = null; if (context instanceof LoadBalancedRetryContext) {
				LoadBalancedRetryContext lbContext = (LoadBalancedRetryContext) context;
				serviceInstance = lbContext.getServiceInstance();

				if (LOG.isDebugEnabled()) {
					LOG.debug(String.format("Retrieved service instance from LoadBalancedRetryContext: %s", serviceInstance));
				}
			}

			//retry loadbalancer if LoadBalancedRetryContext does not has instance
			if (serviceInstance == null) {
				if (LOG.isDebugEnabled()) {
					LOG.debug("Service instance retrieved from LoadBalancedRetryContext: was null. " + "Reattempting service instance selection");
				}
				serviceInstance = loadBalancer.choose(serviceName); if (LOG.isDebugEnabled()) {
					LOG.debug(String.format("Selected service instance: %s", serviceInstance));
				}
			}

			//3. execute request
			ClientHttpResponse response = loadBalancer.execute(serviceName, serviceInstance,
					requestFactory.createRequest(request, body, execution));

			//4. set router context to response
			RouterContextHelper.setRouterContextToResponse(routerContext, response);

			//5. handle response whether retry execute request
			int statusCode = response.getRawStatusCode();
			if (retryPolicy != null && retryPolicy.retryableStatusCode(statusCode)) {
				if (LOG.isDebugEnabled()) {
					LOG.debug(String.format("Retrying on status code: %d", statusCode));
				}
				byte[] bodyCopy = StreamUtils.copyToByteArray(response.getBody());
				response.close();
				throw new ClientHttpResponseStatusCodeException(serviceName, response, bodyCopy);
			}
			return response;
		}, new LoadBalancedRecoveryCallback<ClientHttpResponse, ClientHttpResponse>() {
			// This is a special case, where both parameters to
			// LoadBalancedRecoveryCallback are
			// the same. In most cases they would be different.
			@Override
			protected ClientHttpResponse createResponse(ClientHttpResponse response, URI uri) {
				return response;
			}
		});
	}

	private RetryTemplate createRetryTemplate(String serviceName, HttpRequest request, PolarisRouterContext routerContext,
			LoadBalancedRetryPolicy retryPolicy) {
		RetryTemplate template = new RetryTemplate();
		BackOffPolicy backOffPolicy = lbRetryFactory.createBackOffPolicy(serviceName);
		template.setBackOffPolicy(backOffPolicy == null ? new NoBackOffPolicy() : backOffPolicy);
		template.setThrowLastExceptionOnExhausted(true);
		RetryListener[] retryListeners = lbRetryFactory.createRetryListeners(serviceName);
		if (retryListeners != null && retryListeners.length != 0) {
			template.setListeners(retryListeners);
		}
		//Override: use PolarisInterceptorRetryPolicy for passing router context
		template.setRetryPolicy(!lbProperties.isEnabled() || retryPolicy == null ? new NeverRetryPolicy() :
				new PolarisInterceptorRetryPolicy(request, retryPolicy, loadBalancer, serviceName, routerContext));
		return template;
	}

}
