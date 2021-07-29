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

package com.tencent.cloud.common.util;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/**
 * Spring Context Util
 *
 * @author Hongwei Zhu
 */
@Component
public class ApplicationContextAwareUtils implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ApplicationContextAwareUtils.applicationContext = applicationContext;
    }

    /**
     * 获取上下文
     *
     * @return Spring上下文
     */
    public static ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    /**
     * 获取Spring配置
     *
     * @param key 配置名称
     * @return 配置值
     */
    public static String getProperties(String key) {
        return applicationContext.getEnvironment().getProperty(key);
    }

    /**
     * 获取Spring配置<br>
     * 没有配置时，返回默认值
     *
     * @param key 配置名称
     * @param defaultValue 默认值
     * @return 配置值
     */
    public static String getProperties(String key, String defaultValue) {
        return applicationContext.getEnvironment().getProperty(key, defaultValue);
    }
}
