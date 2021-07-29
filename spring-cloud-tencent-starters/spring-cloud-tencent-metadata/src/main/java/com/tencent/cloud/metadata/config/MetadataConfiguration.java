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

package com.tencent.cloud.metadata.config;

import com.tencent.cloud.feign.PluggableFeign;
import com.tencent.cloud.metadata.constant.MetadataConstant;
import com.tencent.cloud.metadata.core.filter.MetadataReactiveFilter;
import com.tencent.cloud.metadata.core.filter.MetadataServletFilter;
import com.tencent.cloud.metadata.core.interceptor.feign.Metadata2HeaderFeignInterceptor;
import com.tencent.cloud.metadata.core.interceptor.resttemplate.MetadataRestTemplateInterceptor;
import com.tencent.cloud.metadata.core.plugin.feign.MetadataFirstFeignPlugin;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.util.CollectionUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static javax.servlet.DispatcherType.ASYNC;
import static javax.servlet.DispatcherType.ERROR;
import static javax.servlet.DispatcherType.FORWARD;
import static javax.servlet.DispatcherType.INCLUDE;
import static javax.servlet.DispatcherType.REQUEST;

/**
 * Metadata Configuration
 *
 * @author Haotian Zhang
 */
@Configuration
public class MetadataConfiguration {

    /**
     * metadata properties.
     */
    @Bean
    public MetadataLocalProperties metadataLocalProperties() {
        return new MetadataLocalProperties();
    }

    /**
     * Create when web application type is SERVLET.
     */
    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
    static class MetadataServletFilterConfig {

        @Bean
        public FilterRegistrationBean<MetadataServletFilter>
        metadataServletFilterRegistrationBean(MetadataServletFilter metadataServletFilter) {
            FilterRegistrationBean<MetadataServletFilter> filterRegistrationBean =
                    new FilterRegistrationBean<>(metadataServletFilter);
            filterRegistrationBean.setDispatcherTypes(ASYNC, ERROR, FORWARD, INCLUDE, REQUEST);
            filterRegistrationBean.setOrder(MetadataConstant.OrderConstant.FILTER_ORDER);
            return filterRegistrationBean;
        }

        @Bean
        public MetadataServletFilter metadataServletFilter(MetadataLocalProperties metadataLocalProperties) {
            return new MetadataServletFilter(metadataLocalProperties);
        }
    }

    /**
     * Create when web application type is REACTIVE.
     */
    @Configuration
    @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
    static class MetadataReactiveFilterConfig {

        @Bean
        public MetadataReactiveFilter metadataReactiveFilter(MetadataLocalProperties metadataLocalProperties) {
            return new MetadataReactiveFilter(metadataLocalProperties);
        }
    }

    /**
     * Create when Feign exists.
     */
    @Configuration
    @ConditionalOnClass(PluggableFeign.Builder.class)
    static class MetadataFeignPluginConfig {

        @Bean
        public MetadataFirstFeignPlugin metadataFirstFeignPlugin() {
            return new MetadataFirstFeignPlugin();
        }

        @Bean
        public Metadata2HeaderFeignInterceptor metadataFeignInterceptor() {
            return new Metadata2HeaderFeignInterceptor();
        }
    }

    /**
     * Create when RestTemplate exists.
     */
    @Configuration
    @ConditionalOnClass(RestTemplate.class)
    static class MetadataRestTemplateConfig implements ApplicationContextAware {

        private ApplicationContext context;

        @Bean
        public MetadataRestTemplateInterceptor metadataRestTemplateInterceptor() {
            return new MetadataRestTemplateInterceptor();
        }

        @Bean
        BeanPostProcessor metadataRestTemplatePostProcessor(
                MetadataRestTemplateInterceptor metadataRestTemplateInterceptor) {
            // Coping with multiple bean injection scenarios
            Map<String, RestTemplate> beans = this.context.getBeansOfType(RestTemplate.class);
            // If the restTemplate has been created when the MetadataRestTemplatePostProcessor Bean
            // is initialized, then manually set the interceptor.
            if (!CollectionUtils.isEmpty(beans)) {
                for (RestTemplate restTemplate : beans.values()) {
                    List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
                    // Avoid setting interceptor repeatedly.
                    if (null != interceptors && !interceptors.contains(metadataRestTemplateInterceptor)) {
                        interceptors.add(metadataRestTemplateInterceptor);
                        restTemplate.setInterceptors(interceptors);
                    }
                }
            }
            return new MetadataRestTemplatePostProcessor(metadataRestTemplateInterceptor);
        }

        public static class MetadataRestTemplatePostProcessor implements BeanPostProcessor {

            private MetadataRestTemplateInterceptor metadataRestTemplateInterceptor;

            MetadataRestTemplatePostProcessor(
                    MetadataRestTemplateInterceptor metadataRestTemplateInterceptor) {
                this.metadataRestTemplateInterceptor = metadataRestTemplateInterceptor;
            }

            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName) {
                return bean;
            }

            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof RestTemplate) {
                    RestTemplate restTemplate = (RestTemplate) bean;
                    List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
                    // Avoid setting interceptor repeatedly.
                    if (null != interceptors && !interceptors.contains(metadataRestTemplateInterceptor)) {
                        interceptors.add(this.metadataRestTemplateInterceptor);
                        restTemplate.setInterceptors(interceptors);
                    }
                }
                return bean;
            }
        }

        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            this.context = applicationContext;
        }
    }
}
