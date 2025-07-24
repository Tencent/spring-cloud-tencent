/*
 * Copyright (c) 2020 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
 */

package com.tencent.tsf.unit.core.remote;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.kv.model.GetValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.tencent.cloud.common.util.GzipUtil;
import com.tencent.tsf.unit.core.Env;
import com.tencent.tsf.unit.core.TencentUnitManager;
import com.tencent.tsf.unit.core.model.UnitGray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TencentUnitGrayKVLoader {

	private static final Logger LOGGER = LoggerFactory.getLogger(TencentUnitGrayKVLoader.class);
	/**
	 * consul 长轮询等待时间.
	 */
	private final static Integer watchTime = 55;
	/**
	 * 数据当前游标.
	 */
	private static Long index = -1L;
	private static String rawContent;

	private TencentUnitGrayKVLoader() {
	}

	private static String getUnitGrayKey() {
		return "unit/grayList/data";
	}

	public static void syncUnitGray() {
		String newContent = null;
		try {
			ConsulClient client = TsfUnitConsulManager.getConsulClient();

			if (client == null) {
				LOGGER.warn("[syncUnitGray] tsf unit consul client is null");
				return;
			}
			Response<GetValue> response = client.getKVValue(getUnitGrayKey(),
					Env.getConsulToken(), new QueryParams(watchTime, index));

			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("[syncUnitGray] resp:{}", response);
			}
			// data not change
			if (response.getConsulIndex() == null || index.equals(response.getConsulIndex())) {
				return;
			}
			index = response.getConsulIndex();
			// 推空保护，启动后理论上不存在单元化配置被删除的场景
			if (response.getValue() != null) {
				// 控制台 gzip 压缩加 base64 转换，这里进行解压
				newContent = GzipUtil.base64DecodeDecompress(response.getValue().getDecodedValue());
				loadUnitGray(newContent);
			}
		}
		catch (Throwable t) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("[syncUnitGray] newContent:{} error:", newContent, t);
			}
			else {
				LOGGER.warn("[syncUnitGray] error: {}", t.getMessage());
			}
		}
	}

	public static void loadUnitGray(String content) throws JsonProcessingException {
		LOGGER.info("[unit] unit gray old raw content:\n{}", rawContent);

		ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		TencentUnitManager.setUnitGray(mapper.readValue(content, UnitGray.class));

		rawContent = content;
		LOGGER.info("[unit] unit gray new raw content:\n{}", rawContent);
	}

}
