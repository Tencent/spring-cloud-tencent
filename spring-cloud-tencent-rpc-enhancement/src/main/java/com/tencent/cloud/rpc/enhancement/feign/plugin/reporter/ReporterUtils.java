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

package com.tencent.cloud.rpc.enhancement.feign.plugin.reporter;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.Collection;

import com.tencent.cloud.common.constant.RouterConstants;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.polaris.api.pojo.RetStatus;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.api.rpc.ServiceCallResult;
import com.tencent.polaris.api.utils.CollectionUtils;
import feign.Request;
import feign.RequestTemplate;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tencent.cloud.common.constant.ContextConstant.UTF_8;

/**
 * Util for polaris reporter.
 *
 * @author Haotian Zhang
 */
public final class ReporterUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReporterUtils.class);

	private ReporterUtils() {
	}

	public static ServiceCallResult createServiceCallResult(final Request request, RetStatus retStatus) {
		ServiceCallResult resultRequest = new ServiceCallResult();

		resultRequest.setNamespace(MetadataContext.LOCAL_NAMESPACE);
		RequestTemplate requestTemplate = request.requestTemplate();
		String serviceName = requestTemplate.feignTarget().name();
		Collection<String> labels = requestTemplate.headers().get(RouterConstants.ROUTER_LABEL_HEADER);
		if (CollectionUtils.isNotEmpty(labels) && labels.iterator().hasNext()) {
			String label = labels.iterator().next();
			try {
				label = URLDecoder.decode(label, UTF_8);
			}
			catch (UnsupportedEncodingException e) {
				LOGGER.error("unsupported charset exception " + UTF_8, e);
			}
			resultRequest.setLabels(convertLabel(label));
		}
		resultRequest.setService(serviceName);
		URI uri = URI.create(request.url());
		resultRequest.setMethod(uri.getPath());
		resultRequest.setRetStatus(retStatus);
		String sourceNamespace = MetadataContext.LOCAL_NAMESPACE;
		String sourceService = MetadataContext.LOCAL_SERVICE;
		if (StringUtils.isNotBlank(sourceNamespace) && StringUtils.isNotBlank(sourceService)) {
			resultRequest.setCallerService(new ServiceKey(sourceNamespace, sourceService));
		}
		resultRequest.setHost(uri.getHost());
		resultRequest.setPort(uri.getPort());
		return resultRequest;
	}

	private static String convertLabel(String label) {
		label = label.replaceAll("\"|\\{|\\}", "")
				.replaceAll(",", "|");
		return label;
	}
}
