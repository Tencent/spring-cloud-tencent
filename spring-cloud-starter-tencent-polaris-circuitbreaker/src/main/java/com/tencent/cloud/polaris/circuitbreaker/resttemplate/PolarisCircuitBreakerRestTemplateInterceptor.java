package com.tencent.cloud.polaris.circuitbreaker.resttemplate;

import java.io.IOException;
import java.lang.reflect.Method;

import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;

import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

public class PolarisCircuitBreakerRestTemplateInterceptor implements ClientHttpRequestInterceptor {

	private final PolarisCircuitBreakerRestTemplate polarisCircuitBreakerRestTemplate;

	private final ApplicationContext applicationContext;

	private final CircuitBreakerFactory circuitBreakerFactory;

	public PolarisCircuitBreakerRestTemplateInterceptor(
			PolarisCircuitBreakerRestTemplate polarisCircuitBreakerRestTemplate,
			ApplicationContext applicationContext,
			CircuitBreakerFactory circuitBreakerFactory
	) {
		this.polarisCircuitBreakerRestTemplate = polarisCircuitBreakerRestTemplate;
		this.applicationContext = applicationContext;
		this.circuitBreakerFactory = circuitBreakerFactory;
	}

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
		return circuitBreakerFactory.create(request.getURI().getHost() + "#" + request.getURI().getPath()).run(
				() -> {
					try {
						return execution.execute(request, body);
					}
					catch (IOException e) {
						throw new IllegalStateException(e);
					}
				},
				t -> {
					if (StringUtils.hasText(polarisCircuitBreakerRestTemplate.fallback())) {
						CircuitBreakerStatus.FallbackInfo fallbackInfo = new CircuitBreakerStatus.FallbackInfo(200, null, polarisCircuitBreakerRestTemplate.fallback());
						return new PolarisCircuitBreakerHttpResponse(fallbackInfo);
					}
					if (!PolarisCircuitBreakerFallback.class.toGenericString().equals(polarisCircuitBreakerRestTemplate.fallbackClass().toGenericString())) {
						Method method = ReflectionUtils.findMethod(PolarisCircuitBreakerFallback.class, "fallback");
						PolarisCircuitBreakerFallback polarisCircuitBreakerFallback = applicationContext.getBean(polarisCircuitBreakerRestTemplate.fallbackClass());
						return (PolarisCircuitBreakerHttpResponse) ReflectionUtils.invokeMethod(method, polarisCircuitBreakerFallback);
					}
					if (t instanceof CallAbortedException) {
						CircuitBreakerStatus.FallbackInfo fallbackInfo = ((CallAbortedException) t).getFallbackInfo();
						if (fallbackInfo != null) {
							return new PolarisCircuitBreakerHttpResponse(fallbackInfo);
						}
					}
					throw new IllegalStateException(t);
				}
		);
	}

}
