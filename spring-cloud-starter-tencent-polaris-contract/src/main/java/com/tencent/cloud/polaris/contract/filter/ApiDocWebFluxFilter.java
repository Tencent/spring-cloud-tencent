package com.tencent.cloud.polaris.contract.filter;

import com.tencent.cloud.polaris.contract.config.PolarisContractProperties;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import static com.tencent.cloud.polaris.contract.filter.FilterConstant.SWAGGER_RESOURCE_PREFIX;
import static com.tencent.cloud.polaris.contract.filter.FilterConstant.SWAGGER_UI_URL;
import static com.tencent.cloud.polaris.contract.filter.FilterConstant.SWAGGER_V2_API_DOC_URL;
import static com.tencent.cloud.polaris.contract.filter.FilterConstant.SWAGGER_V3_API_DOC_URL;
import static com.tencent.cloud.polaris.contract.filter.FilterConstant.SWAGGER_WEBJARS_V2_PREFIX;
import static com.tencent.cloud.polaris.contract.filter.FilterConstant.SWAGGER_WEBJARS_V3_PREFIX;

/**
 * Filter to disable api doc controller.
 *
 * @author Haotian Zhang
 */
public class ApiDocWebFluxFilter implements WebFilter {

	private final PolarisContractProperties polarisContractProperties;

	public ApiDocWebFluxFilter(PolarisContractProperties polarisContractProperties) {
		this.polarisContractProperties = polarisContractProperties;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange serverWebExchange, @NonNull WebFilterChain webFilterChain) {
		if (!polarisContractProperties.isExposure()) {
			String path = serverWebExchange.getRequest().getURI().getPath();
			if (path.equals(SWAGGER_V2_API_DOC_URL) ||
					path.startsWith(SWAGGER_V3_API_DOC_URL) ||
					path.equals(SWAGGER_UI_URL) ||
					path.startsWith(SWAGGER_RESOURCE_PREFIX) ||
					path.startsWith(SWAGGER_WEBJARS_V2_PREFIX) ||
					path.startsWith(SWAGGER_WEBJARS_V3_PREFIX)) {
				ServerHttpResponse response = serverWebExchange.getResponse();
				response.setRawStatusCode(HttpStatus.FORBIDDEN.value());
				DataBuffer dataBuffer = response.bufferFactory().allocateBuffer();
				return response.writeWith(Mono.just(dataBuffer));
			}
		}
		return webFilterChain.filter(serverWebExchange);
	}
}

