package org.springframework.cloud.client.loadbalancer;

import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpRequest;

public final class LoadBalancerUtils {

	private static final Logger logger = LoggerFactory.getLogger(LoadBalancerUtils.class);

	private LoadBalancerUtils() {
	}

	/**
	 * if request is a BlockingLoadBalancerRequest, return its HttpRequest.
	 */
	public static HttpRequest getHttpRequestIfAvailable(LoadBalancerRequest<?> request) {
		if (request instanceof BlockingLoadBalancerRequest) {
			BlockingLoadBalancerRequest blockingRequest = (BlockingLoadBalancerRequest) request;
			return blockingRequest.getHttpRequest();
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("LoadBalancerRequest is not a BlockingLoadBalancerRequest, request:{}", request);
			}
			return null;
		}
	}

	/**
	 * if request is a LoadBalancerRequestAdapter(RetryLoadBalancerInterceptor), return its delegate.
	 */
	public static LoadBalancerRequest<?> getDelegateLoadBalancerRequestIfAvailable(LoadBalancerRequest<?> request)  {
		if (!(request instanceof LoadBalancerRequestAdapter))  {
			if (logger.isDebugEnabled()) {
				logger.debug("LoadBalancerRequest is not a LoadBalancerRequestAdapter, request:{}", request);
			}
			return request;
		}

		try {
			Field delegateField = LoadBalancerRequestAdapter.class.getDeclaredField("delegate");
			delegateField.setAccessible(true);
			return (LoadBalancerRequest<?>) delegateField.get(request);
		}
		catch (Exception e) {
			// ignore
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to get delegate from LoadBalancerRequestAdapter, request:{}", request, e);
			}
		}
		return null;
	}

}
