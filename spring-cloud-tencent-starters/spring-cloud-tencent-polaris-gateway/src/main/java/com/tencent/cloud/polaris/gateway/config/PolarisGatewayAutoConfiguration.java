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

package com.tencent.cloud.polaris.gateway.config;

import com.tencent.cloud.polaris.gateway.core.scg.filter.Metadata2HeaderScgFilter;
import com.tencent.cloud.polaris.gateway.core.scg.filter.MetadataFirstScgFilter;
import com.tencent.cloud.polaris.gateway.core.scg.filter.RateLimitScgFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Polaris Gateway Auto Configuration
 *
 * @author skyehtzhang
 */
@Configuration
public class PolarisGatewayAutoConfiguration {

    @Configuration()
    @ConditionalOnClass(GlobalFilter.class)
    static class PolarisGatewayScgAutoConfiguration {
        @Bean
        public GlobalFilter metadataFirstScgFilter() {
            return new MetadataFirstScgFilter();
        }

        @Bean
        public GlobalFilter rateLimitScgFilter() {
            return new RateLimitScgFilter();
        }

        @Bean
        public GlobalFilter metadata2HeaderScgFilter() {
            return new Metadata2HeaderScgFilter();
        }
    }

}
