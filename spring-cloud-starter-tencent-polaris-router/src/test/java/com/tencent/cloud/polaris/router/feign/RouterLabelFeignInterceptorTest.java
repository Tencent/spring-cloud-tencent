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

package com.tencent.cloud.polaris.router.feign;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.tencent.cloud.common.constant.OrderConstant;
import com.tencent.cloud.common.constant.RouterConstant;
import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import com.tencent.cloud.common.metadata.StaticMetadataManager;
import com.tencent.cloud.common.util.ApplicationContextAwareUtils;
import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.polaris.context.config.PolarisContextProperties;
import com.tencent.cloud.polaris.router.RouterRuleLabelResolver;
import com.tencent.cloud.polaris.router.spi.FeignRouterLabelResolver;
import feign.RequestTemplate;
import feign.Target;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * test for {@link RouterLabelFeignInterceptor}.
 * @author lepdou 2022-05-26
 */
@ExtendWith(MockitoExtension.class)
public class RouterLabelFeignInterceptorTest {

	@Mock
	private StaticMetadataManager staticMetadataManager;
	@Mock
	private RouterRuleLabelResolver routerRuleLabelResolver;
	@Mock
	private FeignRouterLabelResolver routerLabelResolver;
	@Mock
	private PolarisContextProperties polarisContextProperties;

	@Test
	public void testResolveRouterLabel() throws UnsupportedEncodingException {
		RouterLabelFeignInterceptor routerLabelFeignInterceptor = new RouterLabelFeignInterceptor(
				Collections.singletonList(routerLabelResolver),
				staticMetadataManager, routerRuleLabelResolver, polarisContextProperties);

		assertThat(routerLabelFeignInterceptor.getOrder()).isEqualTo(OrderConstant.Client.Feign.ROUTER_LABEL_INTERCEPTOR_ORDER);

		// mock request template
		RequestTemplate requestTemplate = new RequestTemplate();
		String headerUidKey = "uid";
		String headerUidValue = "1000";
		requestTemplate.header(headerUidKey, headerUidValue);
		String peerService = "peerService";
		Target.EmptyTarget<Object> target = Target.EmptyTarget.create(Object.class, peerService);
		requestTemplate.feignTarget(target);

		// mock ApplicationContextAwareUtils#getProperties
		try (MockedStatic<ApplicationContextAwareUtils> mockedApplicationContextAwareUtils = Mockito.mockStatic(ApplicationContextAwareUtils.class)) {
			String testService = "callerService";
			mockedApplicationContextAwareUtils.when(() -> ApplicationContextAwareUtils.getProperties(anyString()))
					.thenReturn(testService);

			MetadataContext metadataContext = Mockito.mock(MetadataContext.class);

			// mock transitive metadata
			Map<String, String> transitiveLabels = new HashMap<>();
			transitiveLabels.put("k1", "v1");
			transitiveLabels.put("k2", "v22");
			when(metadataContext.getTransitiveMetadata()).thenReturn(transitiveLabels);

			// mock MetadataContextHolder#get
			try (MockedStatic<MetadataContextHolder> mockedMetadataContextHolder = Mockito.mockStatic(MetadataContextHolder.class)) {
				mockedMetadataContextHolder.when(MetadataContextHolder::get).thenReturn(metadataContext);

				// mock expression rule labels
				Set<String> expressionKeys = new HashSet<>();
				expressionKeys.add("${http.header.uid}");
				expressionKeys.add("${http.header.name}");
				when(routerRuleLabelResolver.getExpressionLabelKeys(MetadataContext.LOCAL_NAMESPACE,
						MetadataContext.LOCAL_SERVICE, peerService)).thenReturn(expressionKeys);

				// mock custom resolved labels from request
				Map<String, String> customResolvedLabels = new HashMap<>();
				customResolvedLabels.put("k2", "v2");
				customResolvedLabels.put("k3", "v3");
				when(routerLabelResolver.resolve(requestTemplate, expressionKeys)).thenReturn(customResolvedLabels);

				Map<String, String> localMetadata = new HashMap<>();
				localMetadata.put("k3", "v31");
				localMetadata.put("k4", "v4");
				when(staticMetadataManager.getMergedStaticMetadata()).thenReturn(localMetadata);

				routerLabelFeignInterceptor.apply(requestTemplate);

				Collection<String> routerLabels = requestTemplate.headers().get(RouterConstant.ROUTER_LABEL_HEADER);

				assertThat(routerLabels).isNotNull();
				for (String value : routerLabels) {
					Map<String, String> labels = JacksonUtils.deserialize2Map(URLDecoder.decode(value, "UTF-8"));

					assertThat(labels.get("k1")).isEqualTo("v1");
					assertThat(labels.get("k2")).isEqualTo("v22");
					assertThat(labels.get("k3")).isEqualTo("v3");
					assertThat(labels.get("k4")).isEqualTo("v4");
					assertThat(labels.get("${http.header.uid}")).isEqualTo(headerUidValue);
					assertThat(labels.get("${http.header.name}")).isEqualTo("");
				}
			}
		}
	}
}
