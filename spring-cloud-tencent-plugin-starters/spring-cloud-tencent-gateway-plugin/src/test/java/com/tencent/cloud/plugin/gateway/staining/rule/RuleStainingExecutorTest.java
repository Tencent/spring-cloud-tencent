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

package com.tencent.cloud.plugin.gateway.staining.rule;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import com.tencent.cloud.common.rule.Condition;
import com.tencent.cloud.common.rule.KVPair;
import com.tencent.cloud.common.rule.Operation;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

/**
 * Test for {@link RuleStainingExecutor}.
 * @author lepdou 2022-07-12
 */
@RunWith(MockitoJUnitRunner.class)
public class RuleStainingExecutorTest {

	@Test
	public void testMatchCondition() {
		Condition condition1 = new Condition();
		condition1.setKey("${http.header.uid}");
		condition1.setOperation(Operation.EQUALS.toString());
		condition1.setValues(Collections.singletonList("1000"));

		Condition condition2 = new Condition();
		condition2.setKey("${http.query.source}");
		condition2.setOperation(Operation.IN.toString());
		condition2.setValues(Collections.singletonList("wx"));

		StainingRule.Rule rule = new StainingRule.Rule();
		rule.setConditions(Arrays.asList(condition1, condition2));

		KVPair kvPair = new KVPair();
		kvPair.setKey("env");
		kvPair.setValue("blue");
		rule.setLabels(Collections.singletonList(kvPair));

		StainingRule stainingRule = new StainingRule();
		stainingRule.setRules(Collections.singletonList(rule));

		MockServerHttpRequest request = MockServerHttpRequest.get("/users")
				.queryParam("source", "wx")
				.header("uid", "1000").build();
		MockServerWebExchange exchange = new MockServerWebExchange.Builder(request).build();

		RuleStainingExecutor executor = new RuleStainingExecutor();

		Map<String, String> stainedLabels = executor.execute(exchange, stainingRule);

		Assert.assertNotNull(stainedLabels);
		Assert.assertEquals(1, stainedLabels.size());
		Assert.assertEquals("blue", stainedLabels.get("env"));
	}

	@Test
	public void testNotMatchCondition() {
		Condition condition1 = new Condition();
		condition1.setKey("${http.header.uid}");
		condition1.setOperation(Operation.EQUALS.toString());
		condition1.setValues(Collections.singletonList("1000"));

		Condition condition2 = new Condition();
		condition2.setKey("${http.query.source}");
		condition2.setOperation(Operation.IN.toString());
		condition2.setValues(Collections.singletonList("wx"));

		StainingRule.Rule rule = new StainingRule.Rule();
		rule.setConditions(Arrays.asList(condition1, condition2));

		KVPair kvPair = new KVPair();
		kvPair.setKey("env");
		kvPair.setValue("blue");
		rule.setLabels(Collections.singletonList(kvPair));

		StainingRule stainingRule = new StainingRule();
		stainingRule.setRules(Collections.singletonList(rule));

		MockServerHttpRequest request = MockServerHttpRequest.get("/users")
				.queryParam("source", "wx")
				.header("uid", "10001").build();
		MockServerWebExchange exchange = new MockServerWebExchange.Builder(request).build();

		RuleStainingExecutor executor = new RuleStainingExecutor();

		Map<String, String> stainedLabels = executor.execute(exchange, stainingRule);

		Assert.assertNotNull(stainedLabels);
		Assert.assertEquals(0, stainedLabels.size());
	}

	@Test
	public void testMatchTwoRulesAndNotMatchOneRule() {
		Condition condition1 = new Condition();
		condition1.setKey("${http.header.uid}");
		condition1.setOperation(Operation.EQUALS.toString());
		condition1.setValues(Collections.singletonList("1000"));

		Condition condition2 = new Condition();
		condition2.setKey("${http.query.source}");
		condition2.setOperation(Operation.IN.toString());
		condition2.setValues(Collections.singletonList("wx"));

		// rule1 matched
		StainingRule.Rule rule1 = new StainingRule.Rule();
		rule1.setConditions(Arrays.asList(condition1, condition2));

		KVPair kvPair = new KVPair();
		kvPair.setKey("env");
		kvPair.setValue("blue");
		rule1.setLabels(Collections.singletonList(kvPair));

		// rule2 matched
		StainingRule.Rule rule2 = new StainingRule.Rule();
		rule2.setConditions(Collections.singletonList(condition1));

		KVPair kvPair2 = new KVPair();
		kvPair2.setKey("label1");
		kvPair2.setValue("value1");
		KVPair kvPair3 = new KVPair();
		kvPair3.setKey("label2");
		kvPair3.setValue("value2");
		rule2.setLabels(Arrays.asList(kvPair2, kvPair3));

		// rule3 not matched
		Condition condition3 = new Condition();
		condition3.setKey("${http.query.type}");
		condition3.setOperation(Operation.IN.toString());
		condition3.setValues(Collections.singletonList("wx"));

		StainingRule.Rule rule3 = new StainingRule.Rule();
		rule3.setConditions(Collections.singletonList(condition3));

		KVPair kvPair4 = new KVPair();
		kvPair4.setKey("label3");
		kvPair4.setValue("value3");
		rule3.setLabels(Collections.singletonList(kvPair4));

		StainingRule stainingRule = new StainingRule();
		stainingRule.setRules(Arrays.asList(rule1, rule2, rule3));

		MockServerHttpRequest request = MockServerHttpRequest.get("/users")
				.queryParam("source", "wx")
				.header("uid", "1000").build();
		MockServerWebExchange exchange = new MockServerWebExchange.Builder(request).build();

		RuleStainingExecutor executor = new RuleStainingExecutor();

		Map<String, String> stainedLabels = executor.execute(exchange, stainingRule);

		Assert.assertNotNull(stainedLabels);
		Assert.assertEquals(3, stainedLabels.size());
		Assert.assertEquals("blue", stainedLabels.get("env"));
		Assert.assertEquals("value1", stainedLabels.get("label1"));
		Assert.assertEquals("value2", stainedLabels.get("label2"));
	}
}
