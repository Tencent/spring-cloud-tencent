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

package org.springframework.tsf.core.entity;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Tag implements Serializable {

	/**
	 * update version whenever change the content in tag.
	 */
	public static final int VERSION = 1;

	private String key;

	private String value;

	private Set<ControlFlag> flags = new HashSet<>();

	public Tag(String key, String value, ControlFlag... flags) {
		this.key = key;
		this.value = value;
		this.flags = new HashSet<>(Arrays.asList(flags));
	}

	public Tag() {
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Set<ControlFlag> getFlags() {
		return flags;
	}

	public void setFlags(Set<ControlFlag> flags) {
		this.flags = flags;
	}

	@Override
	public boolean equals(Object object) {
		if (object instanceof Tag) {
			Tag tag = (Tag) object;
			return (key == null ? tag.key == null : key.equals(tag.key))
					&& (flags == null ? tag.flags == null : flags.equals(tag.flags));
		}
		return false;
	}

	@Override
	public int hashCode() {
		return (key == null ? 0 : key.hashCode()) + (flags == null ? 0 : flags.hashCode());
	}

	@Override
	public String toString() {
		return "Tag{" +
				"key='" + key + '\'' +
				", value='" + value + '\'' +
				", flags=" + flags +
				'}';
	}


	public enum ControlFlag {

		/**
		 * tag transitive by all services.
		 */
		TRANSITIVE,

		/**
		 * tag not used in auth.
		 */
		NOT_IN_AUTH,

		/**
		 * tag not used in route.
		 */
		NOT_IN_ROUTE,

		/**
		 * tag not used in trace.
		 */
		NOT_IN_SLEUTH,

		/**
		 * tag not used in lane.
		 */
		NOT_IN_LANE,

		/**
		 * tag not used in unit.
		 */
		IN_UNIT
	}
}
