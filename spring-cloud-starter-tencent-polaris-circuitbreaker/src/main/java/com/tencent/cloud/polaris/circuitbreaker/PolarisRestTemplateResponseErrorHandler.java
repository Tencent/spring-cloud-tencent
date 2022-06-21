package com.tencent.cloud.polaris.circuitbreaker;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.util.ReflectionUtils;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.api.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Objects;

/**
 * @author : wh
 * @date : 2022/6/21 17:25
 * @description:
 */
public class PolarisRestTemplateResponseErrorHandler implements ResponseErrorHandler {

	private static final Logger LOG = LoggerFactory.getLogger(PolarisRestTemplateResponseErrorHandler.class);

	private static final String FileName = "connection";

	private final ConsumerAPI consumerAPI;

	private final PolarisResponseErrorHandler polarisResponseErrorHandler;


	public PolarisRestTemplateResponseErrorHandler(ConsumerAPI consumerAPI, PolarisResponseErrorHandler polarisResponseErrorHandler) {
		this.consumerAPI = consumerAPI;
		this.polarisResponseErrorHandler = polarisResponseErrorHandler;
	}

	@Override
	public boolean hasError(ClientHttpResponse response) {
		return true;
	}

	@Override
	public void handleError(ClientHttpResponse response) throws IOException{
		if (Objects.nonNull(polarisResponseErrorHandler)) {
			if (polarisResponseErrorHandler.hasError(response)) {
				polarisResponseErrorHandler.handleError(response);
			}
		}
	}

	public void handleError(URI url, HttpMethod method, ClientHttpResponse response) {
		ServiceCallResult resultRequest = null;
		try {
			resultRequest = builderServiceCallResult(url, method, response);
		} catch (IOException e) {
			LOG.error("Will report response of {} url {}", response, url, e);
			throw new RuntimeException(e);
		} finally {
			consumerAPI.updateServiceCallResult(resultRequest);
		}
	}

	private ServiceCallResult builderServiceCallResult(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
		ServiceCallResult resultRequest = new ServiceCallResult();
		String serviceName = url.getHost();
		resultRequest.setService(serviceName);
		resultRequest.setNamespace(MetadataContext.LOCAL_NAMESPACE);
		resultRequest.setMethod(url.getPath());
		resultRequest.setRetStatus(RetStatus.RetSuccess);
		String sourceNamespace = MetadataContext.LOCAL_NAMESPACE;
		String sourceService = MetadataContext.LOCAL_SERVICE;
		if (StringUtils.isNotBlank(sourceNamespace) && StringUtils.isNotBlank(sourceService)) {
			resultRequest.setCallerService(new ServiceKey(sourceNamespace, sourceService));
		}
		HttpURLConnection connection = (HttpURLConnection) ReflectionUtils.getFieldValue(response, FileName);
		URL url1 = connection.getURL();
		resultRequest.setHost(url1.getHost());
		resultRequest.setPort(url1.getPort());
		if (response.getStatusCode().is5xxServerError()) {
			resultRequest.setRetStatus(RetStatus.RetFail);
		}
		return resultRequest;
	}

}
