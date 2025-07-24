/*
 * Copyright (c) 2020 www.tencent.com.
 * All Rights Reserved.
 * This program is the confidential and proprietary information of
 * www.tencent.com ("Confidential Information").  You shall not disclose such
 * Confidential Information and shall use it only in accordance with
 * the terms of the license agreement you entered into with www.tencent.com.
 */

package com.tencent.tsf.unit.core.mapping.api;

/**
 * 数据映射服务，根据用户输入，执行转换后输出.
 */
public interface IMappingService {
	// 数据映射服务接口定义
	MappingEntity processMapping(String params);
}
