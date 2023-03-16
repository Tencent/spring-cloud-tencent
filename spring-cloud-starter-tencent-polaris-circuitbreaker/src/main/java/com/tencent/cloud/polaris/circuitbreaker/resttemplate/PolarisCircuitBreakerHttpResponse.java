package com.tencent.cloud.polaris.circuitbreaker.resttemplate;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.tencent.polaris.api.pojo.CircuitBreakerStatus;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.AbstractClientHttpResponse;

public class PolarisCircuitBreakerHttpResponse extends AbstractClientHttpResponse {

	private final CircuitBreakerStatus.FallbackInfo fallbackInfo;

	public PolarisCircuitBreakerHttpResponse(int code){
		this(new CircuitBreakerStatus.FallbackInfo(code, null, null));
	}

	public PolarisCircuitBreakerHttpResponse(int code, String body){
		this(new CircuitBreakerStatus.FallbackInfo(code, null, body));
	}

	public PolarisCircuitBreakerHttpResponse(int code, Map<String, String> headers, String body){
		this(new CircuitBreakerStatus.FallbackInfo(code, headers, body));
	}

	public PolarisCircuitBreakerHttpResponse(CircuitBreakerStatus.FallbackInfo fallbackInfo){
		this.fallbackInfo = fallbackInfo;
	}

	@Override
	public int getRawStatusCode() throws IOException {
		return fallbackInfo.getCode();
	}

	@Override
	public String getStatusText() throws IOException {
		HttpStatus status = HttpStatus.resolve(getRawStatusCode());
		return (status != null ? status.getReasonPhrase() : "");
	}

	@Override
	public void close() {

	}

	@Override
	public InputStream getBody() throws IOException {
		if (fallbackInfo.getBody() != null) {
			return new ByteArrayInputStream(fallbackInfo.getBody().getBytes());
		}
		return null;
	}

	@Override
	public HttpHeaders getHeaders() {
		if (fallbackInfo.getHeaders() != null) {
			HttpHeaders headers = new HttpHeaders();
			fallbackInfo.getHeaders().forEach(headers::add);
			return headers;
		}
		return null;
	}

}
