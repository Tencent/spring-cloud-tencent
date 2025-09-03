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

package com.tencent.tsf.gateway.core.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.tencent.cloud.plugin.gateway.context.Position;
import com.tencent.tsf.gateway.core.TsfGatewayRequest;
import com.tencent.tsf.gateway.core.exception.TsfGatewayException;
import com.tencent.tsf.gateway.core.model.PluginDetail;
import com.tencent.tsf.gateway.core.model.PluginPayload;
import com.tencent.tsf.gateway.core.model.RequestTransformerPluginInfo;
import com.tencent.tsf.gateway.core.model.TagPluginInfo;
import com.tencent.tsf.gateway.core.model.TransformerAction;
import com.tencent.tsf.gateway.core.model.TransformerTag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import org.springframework.tsf.core.TsfContext;
import org.springframework.tsf.core.entity.Tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;

/**
 * Test for {@link PluginUtil}.
 *
 * @author Haotian Zhang
 */
public class PluginUtilTest {

	private TsfGatewayRequest tsfGatewayRequest;
	private PluginPayload pluginPayload;

	@BeforeEach
	void setUp() throws URISyntaxException {
		// init TsfGatewayRequest
		tsfGatewayRequest = new TsfGatewayRequest();
		tsfGatewayRequest.setUri(new URI("http://localhost:8080/test_group/test_namespace/test_service/api/v1/users/123"));

		// set request headers
		Map<String, String> requestHeaders = new HashMap<>();
		requestHeaders.put("x-user-id", "user123");
		requestHeaders.put("authorization", "Bearer token123");
		tsfGatewayRequest.setRequestHeaders(requestHeaders);

		// set request parameters
		Map<String, String[]> parameterMap = new HashMap<>();
		parameterMap.put("userId", new String[] {"123", "456"});
		parameterMap.put("type", new String[] {"admin"});
		tsfGatewayRequest.setParameterMap(parameterMap);

		// set request cookies
		Map<String, String> cookieMap = new HashMap<>();
		cookieMap.put("sessionId", "session123");
		cookieMap.put("lang", "zh-CN");
		tsfGatewayRequest.setCookieMap(cookieMap);

		// init PluginPayload
		pluginPayload = new PluginPayload();
	}

	@Test
	public void testSortPluginDetail() {
		List<PluginDetail> pluginDetails = new ArrayList<>();
		PluginDetail pluginDetail = new PluginDetail();
		pluginDetail.setName("test");
		pluginDetails.add(pluginDetail);
		Stream<PluginDetail> pluginDetailStream = Stream.of(pluginDetails).flatMap(Collection::stream);
		List<PluginDetail> result = PluginUtil.sortPluginDetail(pluginDetailStream);
		assertThat(result).isNotEmpty();
		assertThat(result).hasSize(1);
		assertThat(result.get(0).getName()).isEqualTo("test");
	}

	@Test
	public void testDeserializeTagPluginInfoList() {
		String tagPluginInfoListJson = "[{\"tagPosition\":\"QUERY\",\"preTagName\":\"preTagName\",\"postTagName\":\"postTagName\",\"traceIdEnabled\":\"Y\"}]";
		List<TagPluginInfo> tagPluginInfoList = PluginUtil.deserializeTagPluginInfoList(tagPluginInfoListJson);
		assertThat(tagPluginInfoList).isNotEmpty();
		assertThat(tagPluginInfoList).hasSize(1);
		TagPluginInfo tagPluginInfo = tagPluginInfoList.get(0);
		assertThat(tagPluginInfo.getTagPosition().name()).isEqualTo("QUERY");
		assertThat(tagPluginInfo.getPreTagName()).isEqualTo("preTagName");
		assertThat(tagPluginInfo.getPostTagName()).isEqualTo("postTagName");
		assertThat(tagPluginInfo.getTraceIdEnabled()).isEqualTo("Y");

		assertThatThrownBy(() -> {
			PluginUtil.deserializeTagPluginInfoList("[{\"tagPosition\":\"QUERY\",\"preTagName\":\"preTagName\",\"postTagName\":\"postTagName\",\"traceIdEnabled\":\"Y\"");
		}).isInstanceOf(TsfGatewayException.class);

		assertThatThrownBy(() -> {
			PluginUtil.deserializeTagPluginInfoList("");
		}).isInstanceOf(TsfGatewayException.class);
	}

	@Test
	public void testTransferToTag_EmptyTagPluginList() {
		String emptyJson = "[]";
		PluginPayload result = PluginUtil.transferToTag(emptyJson, tsfGatewayRequest, pluginPayload);
		assertThat(result).isEqualTo(pluginPayload);
	}

	@Test
	public void testTransferToTag_QueryPosition() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {
			String tagPluginJson = "[{\"tagPosition\":\"QUERY\",\"preTagName\":\"userId\",\"postTagName\":\"user_id\",\"traceIdEnabled\":\"N\"}]";

			PluginPayload result = PluginUtil.transferToTag(tagPluginJson, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testTransferToTag_QueryPosition_WithoutPostTagName() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {
			String tagPluginJson = "[{\"tagPosition\":\"QUERY\",\"preTagName\":\"userId\",\"traceIdEnabled\":\"N\"}]";

			PluginPayload result = PluginUtil.transferToTag(tagPluginJson, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testTransferToTag_QueryPosition_NotFound() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {
			String tagPluginJson = "[{\"tagPosition\":\"QUERY\",\"preTagName\":\"nonExistentParam\",\"postTagName\":\"test_tag\",\"traceIdEnabled\":\"N\"}]";

			PluginPayload result = PluginUtil.transferToTag(tagPluginJson, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testTransferToTag_CookiePosition() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {
			String tagPluginJson = "[{\"tagPosition\":\"COOKIE\",\"preTagName\":\"sessionId\",\"postTagName\":\"session_id\",\"traceIdEnabled\":\"N\"}]";

			PluginPayload result = PluginUtil.transferToTag(tagPluginJson, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testTransferToTag_CookiePosition_WithoutPostTagName() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {
			String tagPluginJson = "[{\"tagPosition\":\"COOKIE\",\"preTagName\":\"sessionId\",\"traceIdEnabled\":\"N\"}]";

			PluginPayload result = PluginUtil.transferToTag(tagPluginJson, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testTransferToTag_CookiePosition_NotFound() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {
			String tagPluginJson = "[{\"tagPosition\":\"COOKIE\",\"preTagName\":\"nonExistentCookie\",\"postTagName\":\"test_tag\",\"traceIdEnabled\":\"N\"}]";

			PluginPayload result = PluginUtil.transferToTag(tagPluginJson, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testTransferToTag_HeaderPosition() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {
			String tagPluginJson = "[{\"tagPosition\":\"HEADER\",\"preTagName\":\"x-user-id\",\"postTagName\":\"user_id\",\"traceIdEnabled\":\"N\"}]";

			PluginPayload result = PluginUtil.transferToTag(tagPluginJson, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testTransferToTag_HeaderPosition_WithoutPostTagName() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {
			String tagPluginJson = "[{\"tagPosition\":\"HEADER\",\"preTagName\":\"x-user-id\",\"traceIdEnabled\":\"N\"}]";

			PluginPayload result = PluginUtil.transferToTag(tagPluginJson, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testTransferToTag_HeaderPosition_NotFound() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {
			String tagPluginJson = "[{\"tagPosition\":\"HEADER\",\"preTagName\":\"non-existent-header\",\"postTagName\":\"test_tag\",\"traceIdEnabled\":\"N\"}]";

			PluginPayload result = PluginUtil.transferToTag(tagPluginJson, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testTransferToTag_PathPosition() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {
			String tagPluginJson = "[{\"tagPosition\":\"PATH\",\"preTagName\":\"/api/v1/users/{userId}\",\"postTagName\":\"user_id\",\"traceIdEnabled\":\"N\"}]";

			PluginPayload result = PluginUtil.transferToTag(tagPluginJson, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testTransferToTag_PathPosition_NotMatched() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {
			String tagPluginJson = "[{\"tagPosition\":\"PATH\",\"preTagName\":\"/api/v2/products/{productId}\",\"postTagName\":\"product_id\",\"traceIdEnabled\":\"N\"}]";

			PluginPayload result = PluginUtil.transferToTag(tagPluginJson, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testTransferToTag_PathPosition_WithoutPostTagName() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {
			String tagPluginJson = "[{\"tagPosition\":\"PATH\",\"preTagName\":\"/api/v1/users/{userId}\",\"traceIdEnabled\":\"N\"}]";

			PluginPayload result = PluginUtil.transferToTag(tagPluginJson, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testTransferToTag_PathPosition_EmptyPath() throws Exception {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {
			// set empty path
			tsfGatewayRequest.setUri(new URI("http://localhost:8080"));
			String tagPluginJson = "[{\"tagPosition\":\"PATH\",\"preTagName\":\"/api/v1/users/{userId}\",\"postTagName\":\"user_id\",\"traceIdEnabled\":\"N\"}]";

			PluginPayload result = PluginUtil.transferToTag(tagPluginJson, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testTransferToTag_PathPosition_PathWithoutLeadingSlash() throws Exception {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {
			tsfGatewayRequest.setUri(new URI("http://localhost:8080/api/v1/users/123"));
			String tagPluginJson = "[{\"tagPosition\":\"PATH\",\"preTagName\":\"/api/v1/users/{userId}\",\"postTagName\":\"user_id\",\"traceIdEnabled\":\"N\"}]";

			PluginPayload result = PluginUtil.transferToTag(tagPluginJson, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testTransferToTag_WithTraceIdEnabled() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {
			String tagPluginJson = "[{\"tagPosition\":\"QUERY\",\"preTagName\":\"userId\",\"postTagName\":\"user_id\",\"traceIdEnabled\":\"Y\"}]";

			PluginPayload result = PluginUtil.transferToTag(tagPluginJson, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			// verify that the response headers contain the expected keys
			assertThat(result.getResponseHeaders()).isNotNull();
			assertThat(result.getResponseHeaders()).containsKey("user_id");
			assertThat(result.getResponseHeaders()).containsKey("X-Tsf-TraceId");

			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testTransferToTag_MultipleTagPlugins() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {
			String tagPluginJson = "[" +
					"{\"tagPosition\":\"QUERY\",\"preTagName\":\"userId\",\"postTagName\":\"user_id\",\"traceIdEnabled\":\"N\"}," +
					"{\"tagPosition\":\"HEADER\",\"preTagName\":\"x-user-id\",\"postTagName\":\"header_user_id\",\"traceIdEnabled\":\"N\"}," +
					"{\"tagPosition\":\"COOKIE\",\"preTagName\":\"sessionId\",\"postTagName\":\"session_id\",\"traceIdEnabled\":\"N\"}" +
					"]";

			PluginPayload result = PluginUtil.transferToTag(tagPluginJson, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testTransferToTag_WithExistingResponseHeaders() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {
			// set existing response headers
			Map<String, String> existingHeaders = new HashMap<>();
			existingHeaders.put("existing-header", "existing-value");
			pluginPayload.setResponseHeaders(existingHeaders);

			String tagPluginJson = "[{\"tagPosition\":\"QUERY\",\"preTagName\":\"userId\",\"postTagName\":\"user_id\",\"traceIdEnabled\":\"Y\"}]";

			PluginPayload result = PluginUtil.transferToTag(tagPluginJson, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			// verify that the response headers contain the expected keys
			assertThat(result.getResponseHeaders()).containsKey("existing-header");
			assertThat(result.getResponseHeaders()).containsKey("user_id");
			assertThat(result.getResponseHeaders()).containsKey("X-Tsf-TraceId");

			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testTransferToTag_DefaultPosition() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {
			// create a TagPluginInfo with a non-existent Position
			TagPluginInfo tagPluginInfo = new TagPluginInfo();
			tagPluginInfo.setTagPosition(Position.TSF_TAG); // 使用一个不在switch中处理的Position
			tagPluginInfo.setPreTagName("testTag");
			tagPluginInfo.setPostTagName("test_tag");
			tagPluginInfo.setTraceIdEnabled("N");

			String tagPluginJson = "[{\"tagPosition\":\"TSF_TAG\",\"preTagName\":\"testTag\",\"postTagName\":\"test_tag\",\"traceIdEnabled\":\"N\"}]";

			PluginPayload result = PluginUtil.transferToTag(tagPluginJson, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testPredicateJsonFormat() {
		String tagPluginInfoListJson = "";
		assertThat(PluginUtil.predicateJsonFormat(tagPluginInfoListJson)).isFalse();
	}

	@Test
	public void testDoRequestTransformer_NullRequestTransformerPluginInfo() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {
			PluginPayload result = PluginUtil.doRequestTransformer(null, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verifyNoInteractions();
		}
	}

	@Test
	public void testDoRequestTransformer_EmptyActions() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {
			RequestTransformerPluginInfo pluginInfo = new RequestTransformerPluginInfo();
			pluginInfo.setActions(new ArrayList<>());

			PluginPayload result = PluginUtil.doRequestTransformer(pluginInfo, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verifyNoInteractions();
		}
	}

	@Test
	public void testDoRequestTransformer_NullActions() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {
			RequestTransformerPluginInfo pluginInfo = new RequestTransformerPluginInfo();
			pluginInfo.setActions(null);

			PluginPayload result = PluginUtil.doRequestTransformer(pluginInfo, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verifyNoInteractions();
		}
	}

	@Test
	public void testDoRequestTransformer_NoFilters_TsfTagAction() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {
			RequestTransformerPluginInfo pluginInfo = new RequestTransformerPluginInfo();

			TransformerAction action = new TransformerAction();
			action.setTagPosition(Position.TSF_TAG);
			action.setTagName("test-tag");
			action.setTagValue("test-value");
			action.setWeight(100);

			List<TransformerAction> actions = new ArrayList<>();
			actions.add(action);
			pluginInfo.setActions(actions);

			PluginPayload result = PluginUtil.doRequestTransformer(pluginInfo, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testDoRequestTransformer_NoFilters_HeaderAction() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {
			RequestTransformerPluginInfo pluginInfo = new RequestTransformerPluginInfo();

			TransformerAction action = new TransformerAction();
			action.setTagPosition(Position.HEADER);
			action.setTagName("X-Custom-Header");
			action.setTagValue("custom-value");
			action.setWeight(100);

			List<TransformerAction> actions = new ArrayList<>();
			actions.add(action);
			pluginInfo.setActions(actions);

			PluginPayload result = PluginUtil.doRequestTransformer(pluginInfo, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			assertThat(result.getRequestHeaders()).isNotNull();
			assertThat(result.getRequestHeaders()).containsEntry("X-Custom-Header", "custom-value");

			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testDoRequestTransformer_NoFilters_HeaderAction_WithExistingHeaders() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {
			// set existing request headers
			Map<String, String> existingHeaders = new HashMap<>();
			existingHeaders.put("existing-header", "existing-value");
			pluginPayload.setRequestHeaders(existingHeaders);

			RequestTransformerPluginInfo pluginInfo = new RequestTransformerPluginInfo();

			// create action
			TransformerAction action = new TransformerAction();
			action.setTagPosition(Position.HEADER);
			action.setTagName("X-Custom-Header");
			action.setTagValue("custom-value");
			action.setWeight(100);
			List<TransformerAction> actions = new ArrayList<>();
			actions.add(action);
			pluginInfo.setActions(actions);

			PluginPayload result = PluginUtil.doRequestTransformer(pluginInfo, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			assertThat(result.getRequestHeaders()).containsEntry("existing-header", "existing-value");
			assertThat(result.getRequestHeaders()).containsEntry("X-Custom-Header", "custom-value");

			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testDoRequestTransformer_QueryFilter_Match() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {

			RequestTransformerPluginInfo pluginInfo = new RequestTransformerPluginInfo();

			TransformerTag filter = new TransformerTag();
			filter.setTagPosition(Position.QUERY);
			filter.setTagField("userId");
			filter.setTagValue("123");

			List<TransformerTag> filters = new ArrayList<>();
			filters.add(filter);
			pluginInfo.setFilters(filters);

			TransformerAction action = new TransformerAction();
			action.setTagPosition(Position.TSF_TAG);
			action.setTagName("matched-tag");
			action.setTagValue("matched-value");
			action.setWeight(100);

			List<TransformerAction> actions = new ArrayList<>();
			actions.add(action);
			pluginInfo.setActions(actions);

			PluginPayload result = PluginUtil.doRequestTransformer(pluginInfo, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testDoRequestTransformer_QueryFilter_NotMatch() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {

			RequestTransformerPluginInfo pluginInfo = new RequestTransformerPluginInfo();

			TransformerTag filter = new TransformerTag();
			filter.setTagPosition(Position.QUERY);
			filter.setTagField("userId");
			filter.setTagValue("999");

			List<TransformerTag> filters = new ArrayList<>();
			filters.add(filter);
			pluginInfo.setFilters(filters);

			TransformerAction action = new TransformerAction();
			action.setTagPosition(Position.TSF_TAG);
			action.setTagName("matched-tag");
			action.setTagValue("matched-value");
			action.setWeight(100);

			List<TransformerAction> actions = new ArrayList<>();
			actions.add(action);
			pluginInfo.setActions(actions);

			PluginPayload result = PluginUtil.doRequestTransformer(pluginInfo, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testDoRequestTransformer_QueryFilter_NullQueryValue() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {

			RequestTransformerPluginInfo pluginInfo = new RequestTransformerPluginInfo();

			TransformerTag filter = new TransformerTag();
			filter.setTagPosition(Position.QUERY);
			filter.setTagField("nonExistentParam");
			filter.setTagValue("test");

			List<TransformerTag> filters = new ArrayList<>();
			filters.add(filter);
			pluginInfo.setFilters(filters);

			TransformerAction action = new TransformerAction();
			action.setTagPosition(Position.TSF_TAG);
			action.setTagName("matched-tag");
			action.setTagValue("matched-value");
			action.setWeight(100);

			List<TransformerAction> actions = new ArrayList<>();
			actions.add(action);
			pluginInfo.setActions(actions);

			PluginPayload result = PluginUtil.doRequestTransformer(pluginInfo, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testDoRequestTransformer_CookieFilter_Match() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {

			RequestTransformerPluginInfo pluginInfo = new RequestTransformerPluginInfo();

			TransformerTag filter = new TransformerTag();
			filter.setTagPosition(Position.COOKIE);
			filter.setTagField("sessionId");
			filter.setTagValue("session123");

			List<TransformerTag> filters = new ArrayList<>();
			filters.add(filter);
			pluginInfo.setFilters(filters);

			TransformerAction action = new TransformerAction();
			action.setTagPosition(Position.TSF_TAG);
			action.setTagName("session-tag");
			action.setTagValue("session-value");
			action.setWeight(100);

			List<TransformerAction> actions = new ArrayList<>();
			actions.add(action);
			pluginInfo.setActions(actions);

			PluginPayload result = PluginUtil.doRequestTransformer(pluginInfo, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testDoRequestTransformer_HeaderFilter_Match() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {

			RequestTransformerPluginInfo pluginInfo = new RequestTransformerPluginInfo();

			TransformerTag filter = new TransformerTag();
			filter.setTagPosition(Position.HEADER);
			filter.setTagField("x-user-id");
			filter.setTagValue("user123");

			List<TransformerTag> filters = new ArrayList<>();
			filters.add(filter);
			pluginInfo.setFilters(filters);

			TransformerAction action = new TransformerAction();
			action.setTagPosition(Position.TSF_TAG);
			action.setTagName("user-tag");
			action.setTagValue("user-value");
			action.setWeight(100);

			List<TransformerAction> actions = new ArrayList<>();
			actions.add(action);
			pluginInfo.setActions(actions);

			PluginPayload result = PluginUtil.doRequestTransformer(pluginInfo, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testDoRequestTransformer_PathFilter_Match() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {

			RequestTransformerPluginInfo pluginInfo = new RequestTransformerPluginInfo();

			TransformerTag filter = new TransformerTag();
			filter.setTagPosition(Position.PATH);
			filter.setTagField("/api/v1/users/{userId}");
			filter.setTagValue("123");

			List<TransformerTag> filters = new ArrayList<>();
			filters.add(filter);
			pluginInfo.setFilters(filters);

			TransformerAction action = new TransformerAction();
			action.setTagPosition(Position.TSF_TAG);
			action.setTagName("path-tag");
			action.setTagValue("path-value");
			action.setWeight(100);

			List<TransformerAction> actions = new ArrayList<>();
			actions.add(action);
			pluginInfo.setActions(actions);

			PluginPayload result = PluginUtil.doRequestTransformer(pluginInfo, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testDoRequestTransformer_PathFilter_NotMatch() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {
			RequestTransformerPluginInfo pluginInfo = new RequestTransformerPluginInfo();

			// create filter with not match path
			TransformerTag filter = new TransformerTag();
			filter.setTagPosition(Position.PATH);
			filter.setTagField("/api/v2/products/{productId}");
			filter.setTagValue("123");

			List<TransformerTag> filters = new ArrayList<>();
			filters.add(filter);
			pluginInfo.setFilters(filters);

			TransformerAction action = new TransformerAction();
			action.setTagPosition(Position.TSF_TAG);
			action.setTagName("path-tag");
			action.setTagValue("path-value");
			action.setWeight(100);

			List<TransformerAction> actions = new ArrayList<>();
			actions.add(action);
			pluginInfo.setActions(actions);

			PluginPayload result = PluginUtil.doRequestTransformer(pluginInfo, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testDoRequestTransformer_PathFilter_EmptyPath() throws Exception {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {
			// set empty path
			tsfGatewayRequest.setUri(new URI("http://localhost:8080"));

			RequestTransformerPluginInfo pluginInfo = new RequestTransformerPluginInfo();

			TransformerTag filter = new TransformerTag();
			filter.setTagPosition(Position.PATH);
			filter.setTagField("/api/v1/users/{userId}");
			filter.setTagValue("123");

			List<TransformerTag> filters = new ArrayList<>();
			filters.add(filter);
			pluginInfo.setFilters(filters);

			TransformerAction action = new TransformerAction();
			action.setTagPosition(Position.TSF_TAG);
			action.setTagName("path-tag");
			action.setTagValue("path-value");
			action.setWeight(100);

			List<TransformerAction> actions = new ArrayList<>();
			actions.add(action);
			pluginInfo.setActions(actions);

			PluginPayload result = PluginUtil.doRequestTransformer(pluginInfo, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testDoRequestTransformer_PathFilter_PathWithoutLeadingSlash() throws Exception {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {

			tsfGatewayRequest.setUri(new URI("http://localhost:8080/api/v1/users/123"));

			RequestTransformerPluginInfo pluginInfo = new RequestTransformerPluginInfo();

			TransformerTag filter = new TransformerTag();
			filter.setTagPosition(Position.PATH);
			filter.setTagField("/api/v1/users/{userId}");
			filter.setTagValue("123");

			List<TransformerTag> filters = new ArrayList<>();
			filters.add(filter);
			pluginInfo.setFilters(filters);

			TransformerAction action = new TransformerAction();
			action.setTagPosition(Position.TSF_TAG);
			action.setTagName("path-tag");
			action.setTagValue("path-value");
			action.setWeight(100);

			List<TransformerAction> actions = new ArrayList<>();
			actions.add(action);
			pluginInfo.setActions(actions);

			PluginPayload result = PluginUtil.doRequestTransformer(pluginInfo, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testDoRequestTransformer_MultipleFilters_AllMatch() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {

			RequestTransformerPluginInfo pluginInfo = new RequestTransformerPluginInfo();

			TransformerTag queryFilter = new TransformerTag();
			queryFilter.setTagPosition(Position.QUERY);
			queryFilter.setTagField("userId");
			queryFilter.setTagValue("123");

			TransformerTag headerFilter = new TransformerTag();
			headerFilter.setTagPosition(Position.HEADER);
			headerFilter.setTagField("x-user-id");
			headerFilter.setTagValue("user123");

			List<TransformerTag> filters = new ArrayList<>();
			filters.add(queryFilter);
			filters.add(headerFilter);
			pluginInfo.setFilters(filters);

			TransformerAction action = new TransformerAction();
			action.setTagPosition(Position.TSF_TAG);
			action.setTagName("multi-tag");
			action.setTagValue("multi-value");
			action.setWeight(100);

			List<TransformerAction> actions = new ArrayList<>();
			actions.add(action);
			pluginInfo.setActions(actions);

			PluginPayload result = PluginUtil.doRequestTransformer(pluginInfo, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testDoRequestTransformer_MultipleFilters_OneNotMatch() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {

			RequestTransformerPluginInfo pluginInfo = new RequestTransformerPluginInfo();

			TransformerTag queryFilter = new TransformerTag();
			queryFilter.setTagPosition(Position.QUERY);
			queryFilter.setTagField("userId");
			queryFilter.setTagValue("123");

			TransformerTag headerFilter = new TransformerTag();
			headerFilter.setTagPosition(Position.HEADER);
			headerFilter.setTagField("x-user-id");
			headerFilter.setTagValue("user999");

			List<TransformerTag> filters = new ArrayList<>();
			filters.add(queryFilter);
			filters.add(headerFilter);
			pluginInfo.setFilters(filters);

			TransformerAction action = new TransformerAction();
			action.setTagPosition(Position.TSF_TAG);
			action.setTagName("multi-tag");
			action.setTagValue("multi-value");
			action.setWeight(100);

			List<TransformerAction> actions = new ArrayList<>();
			actions.add(action);
			pluginInfo.setActions(actions);

			PluginPayload result = PluginUtil.doRequestTransformer(pluginInfo, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testDoRequestTransformer_DefaultActionPosition() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {
			RequestTransformerPluginInfo pluginInfo = new RequestTransformerPluginInfo();

			// create action with default position (not TSF_TAG or HEADER)
			TransformerAction action = new TransformerAction();
			action.setTagPosition(Position.QUERY); // 使用一个不在switch中处理的Position
			action.setTagName("test-tag");
			action.setTagValue("test-value");
			action.setWeight(100);

			List<TransformerAction> actions = new ArrayList<>();
			actions.add(action);
			pluginInfo.setActions(actions);

			PluginPayload result = PluginUtil.doRequestTransformer(pluginInfo, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}

	@Test
	public void testDoRequestTransformer_DefaultFilterPosition() {
		try (MockedStatic<TsfContext> mockedTsfContext = Mockito.mockStatic(TsfContext.class)) {
			RequestTransformerPluginInfo pluginInfo = new RequestTransformerPluginInfo();

			// create filter with default position (not in switch cases)
			TransformerTag filter = new TransformerTag();
			filter.setTagPosition(Position.TSF_TAG); // 使用一个不在switch中处理的Position
			filter.setTagField("test-field");
			filter.setTagValue("test-value");

			List<TransformerTag> filters = new ArrayList<>();
			filters.add(filter);
			pluginInfo.setFilters(filters);

			TransformerAction action = new TransformerAction();
			action.setTagPosition(Position.TSF_TAG);
			action.setTagName("test-tag");
			action.setTagValue("test-value");
			action.setWeight(100);

			List<TransformerAction> actions = new ArrayList<>();
			actions.add(action);
			pluginInfo.setActions(actions);

			PluginPayload result = PluginUtil.doRequestTransformer(pluginInfo, tsfGatewayRequest, pluginPayload);

			assertThat(result).isSameAs(pluginPayload);
			mockedTsfContext.verify(() -> TsfContext.putTags(anyMap(), eq(Tag.ControlFlag.TRANSITIVE)));
		}
	}
}
