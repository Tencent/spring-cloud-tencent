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

package com.tencent.tsf.consul.config.watch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tencent.cloud.polaris.config.listener.ConfigChangeEvent;
import com.tencent.cloud.polaris.config.listener.PolarisConfigListenerContext;
import com.tencent.cloud.polaris.config.listener.SyncConfigChangeListener;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;

public class TsfConsulConfigRefreshEventListener implements BeanPostProcessor, PriorityOrdered {
	private static final String DOT = ".";

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Override
	public Object postProcessBeforeInitialization(@NonNull Object obj, @NonNull String beanName) throws BeansException {
		return obj;
	}

	@Override
	public Object postProcessAfterInitialization(@NonNull Object obj, @NonNull String beanName) throws BeansException {
		Class<?> clz = obj.getClass();
		if (!clz.isAnnotationPresent(ConfigChangeListener.class) || !ConfigChangeCallback.class.isAssignableFrom(clz)) {
			return obj;
		}

		ConfigChangeListener targetAnno = clz.getAnnotation(ConfigChangeListener.class);
		String watchedPrefix = targetAnno.prefix();
		String[] watchedConfirmedValue = targetAnno.value();
		boolean isAsync = targetAnno.async();
		if (watchedConfirmedValue.length == 0 && StringUtils.isEmpty(watchedPrefix)) {
			return obj;
		}

		ConfigChangeCallback bean = (ConfigChangeCallback) obj;
		com.tencent.cloud.polaris.config.listener.ConfigChangeListener listener = new SyncConfigChangeListener() {
			@Override
			public void onChange(ConfigChangeEvent changeEvent) {
				List<TsfCallbackParam> paramList = parseConfigChangeEventToTsfCallbackParam(changeEvent);
				for (TsfCallbackParam param : paramList) {
					if (isAsync()) {
						PolarisConfigListenerContext.executor()
								.execute(() -> bean.callback(param.oldValue, param.newValue));
					}
					else {
						bean.callback(param.oldValue, param.newValue);
					}
				}
			}

			@Override
			public boolean isAsync() {
				return isAsync;
			}
		};

		Set<String> interestedKeys = new HashSet<>();
		Set<String> interestedKeyPrefixes = new HashSet<>();
		if (watchedConfirmedValue.length > 0) {
			for (String value : watchedConfirmedValue) {
				interestedKeys.add(StringUtils.isEmpty(watchedPrefix) ? value : watchedPrefix + DOT + value);
			}
		}
		else {
			interestedKeyPrefixes.add(watchedPrefix);
		}

		PolarisConfigListenerContext.addChangeListener(listener, interestedKeys, interestedKeyPrefixes);
		return bean;
	}

	private List<TsfCallbackParam> parseConfigChangeEventToTsfCallbackParam(ConfigChangeEvent event) {
		List<TsfCallbackParam> result = new ArrayList<>();
		Set<String> changedKeys = event.changedKeys();
		for (String changedKey : changedKeys) {
			ConfigProperty oldValue = new ConfigProperty(changedKey, event.getChange(changedKey).getOldValue());
			ConfigProperty newValue = new ConfigProperty(changedKey, event.getChange(changedKey).getNewValue());
			TsfCallbackParam param = new TsfCallbackParam(oldValue, newValue);
			result.add(param);
		}
		return result;
	}

	static class TsfCallbackParam {
		ConfigProperty oldValue;
		ConfigProperty newValue;

		TsfCallbackParam(ConfigProperty oldValue, ConfigProperty newValue) {
			this.oldValue = oldValue;
			this.newValue = newValue;
		}
	}
}
