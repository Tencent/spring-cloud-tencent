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

package com.tencent.cloud.rpc.enhancement.plugin;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;

import com.tencent.cloud.rpc.enhancement.plugin.assembly.AssemblyRequestContext;
import com.tencent.polaris.api.pojo.ServiceKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AssemblyRequestContextTest.
 *
 * @author sean yu
 */
@ExtendWith(MockitoExtension.class)
public class AssemblyRequestContextTest {

	@Test
	public void testAssemblyRequestContext() {
		URI uri = URI.create("http://0.0.0.0/");

		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.add("a", "a");
		httpHeaders.add(HttpHeaders.COOKIE, "cookies-k1=cookies-v1;cookies-k2=cookies-v2");

		EnhancedRequestContext enhancedRequestContext = EnhancedRequestContext.builder()
				.httpMethod(HttpMethod.GET)
				.url(uri)
				.httpHeaders(httpHeaders)
				.build();

		ServiceKey callerService = new ServiceKey("test", "test");
		AssemblyRequestContext assemblyRequestContext = new AssemblyRequestContext(
				enhancedRequestContext,
				callerService,
				"0.0.0.0"
		);

		assertThat(assemblyRequestContext.getURI()).isEqualTo(uri);

		assertThat(assemblyRequestContext.getHeader("a")).isEqualTo("a");
		assemblyRequestContext.setHeader("b", "b");
		assertThat(assemblyRequestContext.listHeaderKeys()).isEqualTo(new HashSet<>(Arrays.asList(HttpHeaders.COOKIE, "a", "b")));

		assertThat(assemblyRequestContext.getMethod()).isEqualTo(HttpMethod.GET.toString());
		assemblyRequestContext.setMethod(HttpMethod.OPTIONS.name());
		assertThat(assemblyRequestContext.getMethod()).isEqualTo(HttpMethod.OPTIONS.toString());

		assertThat(assemblyRequestContext.getCookie("cookies-k1")).isEqualTo("cookies-v1");
		assertThat(assemblyRequestContext.getCookie("cookies-k2")).isEqualTo("cookies-v2");
		assemblyRequestContext.setCookie("cookies-k3", "cookies-v3");
		assertThat(assemblyRequestContext.listCookieKeys()).isEqualTo(new HashSet<>(Arrays.asList("cookies-k1", "cookies-k2", "cookies-k3")));

		assertThat(assemblyRequestContext.getCallerService()).isEqualTo(callerService);
		assertThat(assemblyRequestContext.getCallerIp()).isEqualTo("0.0.0.0");
	}

}
