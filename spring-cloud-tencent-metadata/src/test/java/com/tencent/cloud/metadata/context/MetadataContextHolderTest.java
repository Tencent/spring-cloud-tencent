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

package com.tencent.cloud.metadata.context;

import java.util.HashMap;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import static com.tencent.cloud.metadata.constant.MetadataConstant.SystemMetadataKey.LOCAL_NAMESPACE;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

/**
 * Test for {@link MetadataContextHolder}
 *
 * @author Haotian Zhang
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT,
		classes = MetadataContextHolderTest.TestApplication.class,
		properties = { "spring.config.location = classpath:application-test.yml" })
public class MetadataContextHolderTest {

	@Test
	public void test1() {
		Map<String, String> customMetadata = new HashMap<>();
		customMetadata.put("a", "1");
		customMetadata.put("b", "2");
		MetadataContext metadataContext = MetadataContextHolder.get();
		metadataContext.putAllTransitiveCustomMetadata(customMetadata);
		MetadataContextHolder.set(metadataContext);

		customMetadata = MetadataContextHolder.get().getAllTransitiveCustomMetadata();
		Assertions.assertThat(customMetadata.get("a")).isEqualTo("1");
		Assertions.assertThat(customMetadata.get("b")).isEqualTo("2");

		MetadataContextHolder.remove();

		customMetadata = new HashMap<>();
		customMetadata.put("a", "1");
		customMetadata.put("b", "22");
		customMetadata.put("c", "3");
		Map<String, String> systemMetadata = new HashMap<>();
		systemMetadata.put(LOCAL_NAMESPACE, "namespace");
		MetadataContextHolder.init(customMetadata, systemMetadata);
		metadataContext = MetadataContextHolder.get();
		customMetadata = metadataContext.getAllTransitiveCustomMetadata();
		systemMetadata = metadataContext.getAllSystemMetadata();
		Assertions.assertThat(customMetadata.get("a")).isEqualTo("1");
		Assertions.assertThat(customMetadata.get("b")).isEqualTo("22");
		Assertions.assertThat(customMetadata.get("c")).isEqualTo("3");
		Assertions.assertThat(systemMetadata.get(LOCAL_NAMESPACE)).isEqualTo("namespace");
	}

	@Test
	public void test2() {
		Assertions.assertThat(MetadataContextHolder.LOCAL_NAMESPACE).isEqualTo("default");
		Assertions.assertThat(MetadataContextHolder.LOCAL_SERVICE).isEqualTo("test");
	}

	@SpringBootApplication
	protected static class TestApplication {

	}

}
