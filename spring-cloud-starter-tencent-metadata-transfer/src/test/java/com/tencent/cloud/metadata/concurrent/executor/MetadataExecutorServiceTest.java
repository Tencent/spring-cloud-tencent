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

package com.tencent.cloud.metadata.concurrent.executor;

import com.tencent.cloud.common.metadata.MetadataContext;
import com.tencent.cloud.common.metadata.MetadataContextHolder;
import org.assertj.core.api.Assertions;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Test for {@link MetadataExecutorService}.
 *
 * @author wlx
 * @date 2022/7/9 4:04 下午
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT,
		classes = MetadataExecutorServiceTest.TestApplication.class,
		properties = {"spring.config.location = classpath:application-test.yml",
				"spring.main.web-application-type = servlet",
				"spring.cloud.gateway.enabled = false"})
public class MetadataExecutorServiceTest {

	private static final ExecutorService executorService = Executors.newFixedThreadPool(1);

	@Test
	public void submitTest() throws InterruptedException, ExecutionException, TimeoutException {
		MetadataExecutorService metadataExecutorService = new MetadataExecutorService(executorService);
		Future<Map<String, String>> future = metadataExecutorService.submit(
				() -> {
					return MetadataContextHolder.get().getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);
				}
		);
		Map<String, String> res = future.get(200, TimeUnit.MILLISECONDS);
		Assertions.assertThat(res).isEqualTo(
				MetadataContextHolder.get().getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE)
		);
	}

	@Test
	public void invokeAnyTest() throws ExecutionException, InterruptedException {
		MetadataExecutorService metadataExecutorService = new MetadataExecutorService(executorService);
		Map<String, String> res = metadataExecutorService.invokeAny(getCallableList());
		Assertions.assertThat(res).isEqualTo(
				MetadataContextHolder.get().getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE)
		);
	}

	@Test
	public void invokeAllTest() throws InterruptedException {
		MetadataExecutorService metadataExecutorService = new MetadataExecutorService(executorService);
		List<Future<Map<String, String>>> futures = metadataExecutorService.invokeAll(getCallableList());
		List<Map<String, String>> resList = futures.stream().map(
				future -> {
					try {
						return future.get();
					} catch (InterruptedException | ExecutionException e) {
						return null;
					}
				}
		).collect(Collectors.toList());

		resList.forEach(
				res -> {
					Assertions.assertThat(res).isEqualTo(
							MetadataContextHolder.get().getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE)
					);
				}
		);
	}

	@AfterClass
	public static void cleanUp() {
		executorService.shutdownNow();
	}

	@SpringBootApplication
	protected static class TestApplication {
	}

	private List<Callable<Map<String, String>>> getCallableList() {
		List<Callable<Map<String, String>>> callableList = new ArrayList<>();
		callableList.add(() -> {
			return MetadataContextHolder.get().getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);
		});
		callableList.add(() -> {
			return MetadataContextHolder.get().getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);
		});
		return callableList;
	}
}
