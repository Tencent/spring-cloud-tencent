package com.tencent.cloud.rpc.enhancement.scg;

import java.util.Map;

import com.tencent.cloud.common.constant.HeaderConstant;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.rpc.enhancement.AbstractPolarisReporterAdapter;
import com.tencent.cloud.rpc.enhancement.config.RpcEnhancementReporterProperties;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.plugin.circuitbreaker.ResourceStat;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.circuitbreak.api.CircuitBreakAPI;
import com.tencent.polaris.client.api.SDKContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_LOADBALANCER_RESPONSE_ATTR;

public class PolarisGatewayReporter extends AbstractPolarisReporterAdapter implements GlobalFilter {

	private static final Logger LOG = LoggerFactory.getLogger(PolarisGatewayReporter.class);

	private final ConsumerAPI consumerAPI;

	private final CircuitBreakAPI circuitBreakAPI;

	/**
	 * Constructor With {@link RpcEnhancementReporterProperties} .
	 *
	 * @param reportProperties instance of {@link RpcEnhancementReporterProperties}.
	 */
	public PolarisGatewayReporter(RpcEnhancementReporterProperties reportProperties,
			SDKContext context,
			ConsumerAPI consumerAPI,
			CircuitBreakAPI circuitBreakAPI) {
		super(reportProperties, context);
		this.consumerAPI = consumerAPI;
		this.circuitBreakAPI = circuitBreakAPI;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
		if (!reportProperties.isEnabled()) {
			return chain.filter(exchange);
		}
		long startTime = System.currentTimeMillis();
		return chain.filter(exchange)
				.doOnSuccess(v -> instrumentResponse(exchange, null, startTime))
				.doOnError(t -> instrumentResponse(exchange, t, startTime));
	}

	private void instrumentResponse(ServerWebExchange exchange, Throwable t, long startTime) {
		ServerHttpResponse response = exchange.getResponse();
		ServerHttpRequest request = exchange.getRequest();

		long delay = System.currentTimeMillis() - startTime;
		String serviceId = null;
		String targetHost = null;
		Integer targetPort = null;

		Response<ServiceInstance> serviceInstanceResponse = exchange.getAttribute(GATEWAY_LOADBALANCER_RESPONSE_ATTR);
		if (serviceInstanceResponse != null && serviceInstanceResponse.hasServer()) {
			ServiceInstance instance = serviceInstanceResponse.getServer();
			serviceId = instance.getServiceId();
			targetHost = instance.getHost();
			targetPort = instance.getPort();
		}

		ServiceCallResult resultRequest = createServiceCallResult(
				serviceId,
				targetHost,
				targetPort,
				request.getURI(),
				request.getHeaders(),
				response.getHeaders(),
				response.getRawStatusCode(),
				delay,
				t
		);
		LOG.debug("Will report result of {}. Request=[{} {}]. Response=[{}]. Delay=[{}]ms.",
				resultRequest.getRetStatus().name(), request.getMethod(), request.getURI().getPath(), response.getRawStatusCode(), delay);
		consumerAPI.updateServiceCallResult(resultRequest);

		ResourceStat resourceStat = createInstanceResourceStat(
				serviceId,
				targetHost,
				targetPort,
				request.getURI(),
				response.getRawStatusCode(),
				delay,
				t
		);
		circuitBreakAPI.report(resourceStat);
	}

}
