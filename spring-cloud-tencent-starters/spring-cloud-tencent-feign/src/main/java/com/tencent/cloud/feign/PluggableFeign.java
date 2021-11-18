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

package com.tencent.cloud.feign;

import com.tencent.cloud.common.util.ReflectionUtils;
import feign.Contract;
import feign.Feign;
import feign.InvocationHandlerFactory;
import feign.Target;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.cloud.openfeign.FeignClientFactoryBean;
import org.springframework.cloud.openfeign.FeignContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.util.StringUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Pluggable Feign builder.
 *
 * @author Haotian Zhang
 */
public class PluggableFeign {

    private static final Logger LOG = LoggerFactory.getLogger(PluggableFeign.class);

    private PluggableFeign() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends Feign.Builder implements ApplicationContextAware {

        private Contract contract = new Contract.Default();

        private ApplicationContext applicationContext;

        private FeignContext feignContext;

        @Override
        public Feign.Builder invocationHandlerFactory(InvocationHandlerFactory invocationHandlerFactory) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Builder contract(Contract contract) {
            this.contract = contract;
            return this;
        }

        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            this.applicationContext = applicationContext;
            feignContext = applicationContext.getBean(FeignContext.class);
        }

        @Override
        public Feign build() {
            super.invocationHandlerFactory(new InvocationHandlerFactory() {
                @Override
                public InvocationHandler create(Target target, Map<Method, MethodHandler> dispatch) {
                    GenericApplicationContext gctx = (GenericApplicationContext) Builder.this.applicationContext;
                    BeanDefinition def = gctx.getBeanDefinition(target.type().getName());

                    FeignClientFactoryBean feignClientFactoryBean = (FeignClientFactoryBean) def.getAttribute("feignClientsRegistrarFactoryBean");

                    Class fallback = (Class) ReflectionUtils.getFieldValue(feignClientFactoryBean, "fallback");
                    Class fallbackFactory = (Class) ReflectionUtils.getFieldValue(feignClientFactoryBean,
                            "fallbackFactory");
                    String beanName = (String) ReflectionUtils.getFieldValue(feignClientFactoryBean, "contextId");
                    if (!StringUtils.hasText(beanName)) {
                        beanName = (String) ReflectionUtils.getFieldValue(feignClientFactoryBean, "name");
                    }

                    Object fallbackInstance;
                    FallbackFactory fallbackFactoryInstance;

                    // Get FeignPlugins
                    List<PluggableFeignPlugin> pluggableFeignPlugins = getSortedFeignPrePlugins();

                    if (void.class != fallback) {
                        fallbackInstance = getFallbackInstanceFromContext(beanName, "fallback", fallback,
                                target.type());
                        return new PluggableFeignInvocationHandler(target, dispatch, new FallbackFactory.Default(fallbackInstance), pluggableFeignPlugins);
                    }

                    if (void.class != fallbackFactory) {
                        fallbackFactoryInstance = (FallbackFactory) getFallbackInstanceFromContext(beanName,
                                "fallbackFactory",
                                fallbackFactory, FallbackFactory.class);
                        return new PluggableFeignInvocationHandler(target, dispatch, fallbackFactoryInstance,
                                pluggableFeignPlugins);
                    }

                    return new PluggableFeignInvocationHandler(target, dispatch, null, pluggableFeignPlugins);
                }

                private Object getFallbackInstanceFromContext(String name, String type, Class fallbackType,
                                                              Class targetType) {
                    if (feignContext == null) {
                        feignContext = applicationContext.getBean(FeignContext.class);
                    }
                    Object fallbackInstance = feignContext.getInstance(name, fallbackType);
                    if (fallbackInstance == null) {
                        throw new IllegalStateException(String.format("No %s instance of type %s found for feign "
                                        + "client %s",
                                type, fallbackType, name));
                    }

                    if (!targetType.isAssignableFrom(fallbackType)) {
                        throw new IllegalStateException(String.format(
                                "Incompatible %s instance. Fallback/fallbackFactory of type %s is not assignable to "
                                        + "%s for feign client %s",
                                type, fallbackType, targetType, name));
                    }
                    return fallbackInstance;
                }

                /**
                 * Ascending, which means the lower order number, the earlier executing the feign plugin.
                 *
                 * @return sorted feign pre plugin list
                 */
                private List<PluggableFeignPlugin> getSortedFeignPrePlugins() {
                    Map<String, PluggableFeignPlugin> feignPrePluginMap =
                            applicationContext.getBeansOfType(PluggableFeignPlugin.class);
                    return new ArrayList<>(feignPrePluginMap.values())
                            .stream()
                            .sorted(Comparator.comparing(PluggableFeignPlugin::getOrder))
                            .collect(Collectors.toList());
                }
            });

            super.contract(new PluggableFeignContractHolder(contract));
            return super.build();
        }
    }

}
