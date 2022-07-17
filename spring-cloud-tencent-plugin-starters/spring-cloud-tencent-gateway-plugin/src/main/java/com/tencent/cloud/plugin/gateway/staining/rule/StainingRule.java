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

import java.util.List;

import com.tencent.cloud.common.rule.Condition;
import com.tencent.cloud.common.rule.KVPair;

/**
 * The rules for staining.
 * @author lepdou 2022-07-07
 */
public class StainingRule {

	private List<Rule> rules;

	public List<Rule> getRules() {
		return rules;
	}

	public void setRules(List<Rule> rules) {
		this.rules = rules;
	}

	@Override
	public String toString() {
		return "StainingRule{" +
				"rules=" + rules +
				'}';
	}

	public static class Rule {
		private List<Condition> conditions;
		private List<KVPair> labels;

		public List<Condition> getConditions() {
			return conditions;
		}

		public void setConditions(List<Condition> conditions) {
			this.conditions = conditions;
		}

		public List<KVPair> getLabels() {
			return labels;
		}

		public void setLabels(List<KVPair> labels) {
			this.labels = labels;
		}

		@Override
		public String toString() {
			return "Rule{" +
					"conditions=" + conditions +
					", labels=" + labels +
					'}';
		}
	}

}
