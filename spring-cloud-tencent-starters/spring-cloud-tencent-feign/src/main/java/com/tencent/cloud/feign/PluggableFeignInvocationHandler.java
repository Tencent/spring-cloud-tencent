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

import feign.FeignException;
import feign.InvocationHandlerFactory;
import feign.Target;
import feign.hystrix.FallbackFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static feign.Util.checkNotNull;

/**
 * InvocationHandler used by PluggableFeign.
 *
 * @author Haotian Zhang
 */
public class PluggableFeignInvocationHandler implements InvocationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PluggableFeignInvocationHandler.class);

    private final Target<?> target;

    private final Map<Method, InvocationHandlerFactory.MethodHandler> dispatch;

    private FallbackFactory fallbackFactory;

    private Map<Method, Method> fallbackMethodMap;

    private List<PluggableFeignPlugin> prePluggableFeignPlugins;

    private List<PluggableFeignPlugin> postPluggableFeignPlugins;

    private List<PluggableFeignPlugin> exceptionPluggableFeignPlugins;

    PluggableFeignInvocationHandler(Target<?> target, Map<Method, InvocationHandlerFactory.MethodHandler> dispatch,
                                    FallbackFactory fallbackFactory, List<PluggableFeignPlugin> pluggableFeignPlugins) {
        this.target = checkNotNull(target, "target");
        this.dispatch = checkNotNull(dispatch, "dispatch");
        this.fallbackFactory = fallbackFactory;
        this.fallbackMethodMap = toFallbackMethod(dispatch);

        this.prePluggableFeignPlugins = new ArrayList<>();
        this.postPluggableFeignPlugins = new ArrayList<>();
        this.exceptionPluggableFeignPlugins = new ArrayList<>();
        for (PluggableFeignPlugin feignPlugin : pluggableFeignPlugins) {
            if (feignPlugin.getType().equals(PluggableFeignPluginType.PRE)) {
                prePluggableFeignPlugins.add(feignPlugin);
            } else if (feignPlugin.getType().equals(PluggableFeignPluginType.POST)) {
                postPluggableFeignPlugins.add(feignPlugin);
            } else if (feignPlugin.getType().equals(PluggableFeignPluginType.EXCEPTION)) {
                exceptionPluggableFeignPlugins.add(feignPlugin);
            }
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if ("equals".equals(method.getName())) {
            try {
                Object otherHandler = args.length > 0 && args[0] != null ? Proxy.getInvocationHandler(args[0]) : null;
                return equals(otherHandler);
            } catch (IllegalArgumentException e) {
                return false;
            }
        } else if ("hashCode".equals(method.getName())) {
            return hashCode();
        } else if ("toString".equals(method.getName())) {
            return toString();
        }

        Object result = null;
        PluggableFeignContext context = new PluggableFeignContext();
        try {
            context.setTarget(target);
            context.setDispatch(dispatch);
            context.setFallbackFactory(fallbackFactory);
            context.setFallbackMethodMap(fallbackMethodMap);
            context.setProxy(proxy);
            context.setMethod(method);
            context.setArgs(args);

            // executing pre plugins
            for (PluggableFeignPlugin prePlugin : this.prePluggableFeignPlugins) {
                prePlugin.run(context);
            }

            result = this.dispatch.get(method).invoke(args);

            context.setResult(result);

            // executing post plugins
            for (PluggableFeignPlugin postPlugin : this.postPluggableFeignPlugins) {
                postPlugin.run(context);
            }
        } catch (Throwable throwable) {
            if (throwable.getCause() instanceof FeignException) {
                context.setFeignException((FeignException) throwable.getCause());
            }

            // executing exception plugins
            for (PluggableFeignPlugin exceptionPlugin : this.exceptionPluggableFeignPlugins) {
                exceptionPlugin.run(context);
            }

            // executing fallback logic
            if (this.fallbackFactory != null) {
                return this.fallbackMethodMap.get(method).invoke(fallbackFactory.create(throwable), args);
            } else {
                LOG.error("FallbackFactory is null!");
                throw throwable;
            }
        }

        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PluggableFeignInvocationHandler) {
            PluggableFeignInvocationHandler other = (PluggableFeignInvocationHandler) obj;
            return target.equals(other.target);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return target.hashCode();
    }

    @Override
    public String toString() {
        return target.toString();
    }

    static Map<Method, Method> toFallbackMethod(Map<Method, InvocationHandlerFactory.MethodHandler> dispatch) {
        Map<Method, Method> result = new LinkedHashMap<>();
        if (!CollectionUtils.isEmpty(dispatch)) {
            for (Method method : dispatch.keySet()) {
                method.setAccessible(true);
                result.put(method, method);
            }
        }
        return result;
    }
}
