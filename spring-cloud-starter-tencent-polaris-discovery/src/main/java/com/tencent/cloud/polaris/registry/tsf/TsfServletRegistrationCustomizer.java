/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package com.tencent.cloud.polaris.registry.tsf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import com.tencent.cloud.common.util.JacksonUtils;
import com.tencent.cloud.polaris.registry.PolarisRegistration;
import com.tencent.cloud.polaris.registry.PolarisRegistrationCustomizer;
import com.tencent.polaris.plugins.connector.common.constant.ConsulConstant;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.util.StringUtils;

import static com.tencent.polaris.plugins.connector.common.constant.ConsulConstant.MetadataMapKey.TAGS_KEY;

/**
 * @author Piotr Wielgolaski
 */
public class TsfServletRegistrationCustomizer implements PolarisRegistrationCustomizer {
	private final ObjectProvider<ServletContext> servletContext;

	public TsfServletRegistrationCustomizer(ObjectProvider<ServletContext> servletContext) {
		this.servletContext = servletContext;
	}

	@Override
	public void customize(PolarisRegistration registration) {
		if (servletContext == null) {
			return;
		}
		ServletContext sc = servletContext.getIfAvailable();
		if (sc != null
				&& StringUtils.hasText(sc.getContextPath())
				&& StringUtils.hasText(sc.getContextPath().replaceAll("/", ""))) {
			Map<String, String> metadata = registration.getMetadata();

			List<String> tags = Arrays.asList(JacksonUtils.deserialize(metadata.get(TAGS_KEY), String[].class));
			if (tags == null) {
				tags = new ArrayList<>();
			}
			tags.add("contextPath=" + sc.getContextPath());
			metadata.put(ConsulConstant.MetadataMapKey.TAGS_KEY, JacksonUtils.serialize2Json(tags));
		}
	}
}
