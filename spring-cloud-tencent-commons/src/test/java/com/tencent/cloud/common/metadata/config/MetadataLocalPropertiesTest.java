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

package com.tencent.cloud.common.metadata.config;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Test for {@link MetadataLocalProperties}.
 *
 * @author Haotian Zhang
 */
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		classes = MetadataLocalPropertiesTest.TestApplication.class,
		properties = {"spring.config.location = classpath:application-test.yml"})
public class MetadataLocalPropertiesTest {

	@Autowired
	private MetadataLocalProperties metadataLocalProperties;

	@Test
	public void test1() {
		Assertions.assertThat(metadataLocalProperties.getContent().get("a")).isEqualTo("1");
		Assertions.assertThat(metadataLocalProperties.getContent().get("b")).isEqualTo("2");
		Assertions.assertThat(metadataLocalProperties.getContent().get("c")).isNull();
	}

	@Test
	public void test2() {
		Assertions.assertThat(metadataLocalProperties.getTransitive().contains("b")).isTrue();
	}

	@Test
	public void test3() {
		Assertions.assertThat(metadataLocalProperties.getHeaders().contains("c")).isTrue();
		Assertions.assertThat(metadataLocalProperties.getHeaders().contains("d")).isTrue();
	}

	@SpringBootApplication
	protected static class TestApplication {

	}
}
