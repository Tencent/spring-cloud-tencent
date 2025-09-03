/*
 * Tencent is pleased to support the open source community by making spring-cloud-tencent available.
 *
 * Copyright (C) 2021 Tencent. All rights reserved.
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

package com.tencent.cloud.common.util;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.tsf.core.entity.Metadata;
import org.springframework.tsf.core.entity.Tag;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * test for {@link TsfTagUtils}.
 */
public class TsfTagUtilsTest {

	@Test
	public void testDeserializeTagList() {
		String data = "%5B%7B%22k%22%3A%22tsf-gateway-ratelimit-context%22%2C%22v%22%3A%22grp-vyiwvq5t%22%2C%22f%22%3A%5B%5D%7D%2C%7B%22k%22%3A%22feat%22%2C%22v%22%3A%22test%22%2C%22f%22%3A%5B%220%22%5D%7D%5D";
		List<Tag> tagList = TsfTagUtils.deserializeTagList(data);
		for (Tag tag : tagList) {
			assertThat(tag.getKey()).isNotNull();
			assertThat(tag.getValue()).isNotNull();
			assertThat(tag.getFlags()).isNotNull();
		}

		assertThat(TsfTagUtils.deserializeTagList(null)).isNull();
		assertThat(TsfTagUtils.deserializeTagList("")).isNull();
	}

	@Test
	public void testDeserializeMetadata() {
		String encodedInput = "%7B%22ai%22%3A%22applicationId%22%2C%22av%22%3A%22applicationVersion%22%2C%22sn%22%3A%22serviceName%22%2C%22ii%22%3A%22instanceId%22%2C%22gi%22%3A%22groupId%22%2C%22li%22%3A%22127.0.0.1%22%2C%22lis%22%3A%22127.0.0.1%22%2C%22ni%22%3A%22namespaceId%22%2C%22pi%22%3Atrue%7D";
		Metadata expectedMetadata = new Metadata();
		expectedMetadata.setApplicationId("applicationId");
		expectedMetadata.setApplicationVersion("applicationVersion");
		expectedMetadata.setServiceName("serviceName");
		expectedMetadata.setInstanceId("instanceId");
		expectedMetadata.setGroupId("groupId");
		expectedMetadata.setLocalIp("127.0.0.1");
		expectedMetadata.setLocalIps("127.0.0.1");
		expectedMetadata.setNamespaceId("namespaceId");
		expectedMetadata.setPreferIpv6(true);

		String json = JacksonUtils.serialize2Json(expectedMetadata);
		String buffer = UrlUtils.encode(json);
		System.out.println(buffer);

		Metadata result = TsfTagUtils.deserializeMetadata(encodedInput);
		assertThat(result).isNotNull();
		assertThat(result.toString()).isEqualTo(expectedMetadata.toString());
		assertThat(result.getApplicationId()).isEqualTo(expectedMetadata.getApplicationId());
		assertThat(result.getApplicationVersion()).isEqualTo(expectedMetadata.getApplicationVersion());
		assertThat(result.getServiceName()).isEqualTo(expectedMetadata.getServiceName());
		assertThat(result.getInstanceId()).isEqualTo(expectedMetadata.getInstanceId());
		assertThat(result.getGroupId()).isEqualTo(expectedMetadata.getGroupId());
		assertThat(result.getLocalIp()).isEqualTo(expectedMetadata.getLocalIp());
		assertThat(result.getLocalIps()).isEqualTo(expectedMetadata.getLocalIps());
		assertThat(result.getNamespaceId()).isEqualTo(expectedMetadata.getNamespaceId());
		assertThat(result.isPreferIpv6()).isEqualTo(expectedMetadata.isPreferIpv6());

		assertThat(TsfTagUtils.deserializeMetadata(null)).isNull();
		assertThat(TsfTagUtils.deserializeMetadata("")).isNull();
	}
}
