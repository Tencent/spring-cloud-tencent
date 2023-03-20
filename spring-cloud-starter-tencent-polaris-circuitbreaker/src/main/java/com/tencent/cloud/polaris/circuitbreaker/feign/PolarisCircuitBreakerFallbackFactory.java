package com.tencent.cloud.polaris.circuitbreaker.feign;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.tencent.polaris.api.pojo.CircuitBreakerStatus;
import com.tencent.polaris.circuitbreak.client.exception.CallAbortedException;
import feign.Request;
import feign.RequestTemplate;
import feign.Response;
import feign.codec.Decoder;

import org.springframework.cloud.openfeign.FallbackFactory;

public class PolarisCircuitBreakerFallbackFactory implements FallbackFactory {

	private final Decoder decoder;

	public PolarisCircuitBreakerFallbackFactory(Decoder decoder) {
		this.decoder = decoder;
	}

	@Override
	public Object create(Throwable t) {
		return new DefaultFallback(t, decoder);
	}

	public class DefaultFallback {

		private final Throwable t;

		private final Decoder decoder;

		public DefaultFallback(Throwable t, Decoder decoder) {
			this.t = t;
			this.decoder = decoder;
		}

		public Object fallback(Method method) {
			if (t instanceof CallAbortedException) {
				CircuitBreakerStatus.FallbackInfo fallbackInfo = ((CallAbortedException) t).getFallbackInfo();
				if (fallbackInfo != null) {
					Response.Builder responseBuilder = Response.builder()
							.status(fallbackInfo.getCode());
					if (fallbackInfo.getHeaders() != null) {
						Map<String, Collection<String>> headers = new HashMap<>();
						fallbackInfo.getHeaders().forEach((k, v) -> {
							if (headers.containsKey(k)) {
								headers.get(k).add(v);
							}
							else {
								headers.put(k, new HashSet<>(Collections.singleton(v)));
							}
						});
						responseBuilder.headers(headers);
					}
					if (fallbackInfo.getBody() != null) {
						responseBuilder.body(fallbackInfo.getBody(), StandardCharsets.UTF_8);
					}
					// Feign Response need a nonnull Request, which is not important in fallback response, so we create a fake one
					Request request = Request.create(Request.HttpMethod.GET, "/", new HashMap<>(), Request.Body.empty(), new RequestTemplate());
					responseBuilder.request(request);
					try (Response response = responseBuilder.build()) {
						return decoder.decode(response, method.getGenericReturnType());
					}
					catch (IOException e) {
						throw new IllegalStateException(e);
					}
				}
			}
			throw new IllegalStateException(t);
		}
	}
}
