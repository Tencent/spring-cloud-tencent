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

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Test for {@link MetadataScheduledExecutorService}.
 *
 * @author wlx
 * @date 2022/7/9 4:04 下午
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT,
		classes = MetadataScheduledExecutorServiceTest.TestApplication.class,
		properties = {"spring.config.location = classpath:application-test.yml",
				"spring.main.web-application-type = servlet",
				"spring.cloud.gateway.enabled = false"})
public class MetadataScheduledExecutorServiceTest {

	private static final ScheduledExecutorService scheduledExecutorService =
			Executors.newSingleThreadScheduledExecutor();

	@Test
	public void scheduleRunnableTest() throws ExecutionException, InterruptedException {
		Map<String, String> fragmentContext =
				MetadataContextHolder.get().getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);

		ScheduledFuture<?> schedule = scheduledExecutorService.schedule(() -> {
			Assertions.assertThat(fragmentContext).
					isEqualTo(MetadataContextHolder.get().getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE));
		}, 100, TimeUnit.MILLISECONDS);
		schedule.get();
	}

	@Test
	public void scheduleCallableTest() throws ExecutionException, InterruptedException {
		ScheduledFuture<Map<String, String>> schedule = scheduledExecutorService.schedule(() -> {
			return MetadataContextHolder.get().getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE);
		}, 100, TimeUnit.MILLISECONDS);
		Map<String, String> res = schedule.get();
		Assertions.assertThat(res).isEqualTo(
				MetadataContextHolder.get().getFragmentContext(MetadataContext.FRAGMENT_TRANSITIVE)
		);
	}

	@AfterClass
	public static void cleanUp() {
		scheduledExecutorService.shutdownNow();
	}

	@SpringBootApplication
	protected static class TestApplication {
	}
}
