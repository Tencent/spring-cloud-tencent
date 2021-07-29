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

import java.lang.reflect.Method;
import java.util.Map;

/**
 * Context used by PluggableFeign
 *
 * @author Haotian Zhang
 */
public class PluggableFeignContext {

    private Target<?> target;

    private Map<Method, InvocationHandlerFactory.MethodHandler> dispatch;

    private FallbackFactory fallbackFactory;

    private Map<Method, Method> fallbackMethodMap;

    private Object proxy;

    private Method method;

    private Object[] args;

    private FeignException feignException;

    private Object result;

    public Target<?> getTarget() {
        return target;
    }

    public void setTarget(Target<?> target) {
        this.target = target;
    }

    public Map<Method, InvocationHandlerFactory.MethodHandler> getDispatch() {
        return dispatch;
    }

    public void setDispatch(Map<Method, InvocationHandlerFactory.MethodHandler> dispatch) {
        this.dispatch = dispatch;
    }

    public FallbackFactory getFallbackFactory() {
        return fallbackFactory;
    }

    public void setFallbackFactory(FallbackFactory fallbackFactory) {
        this.fallbackFactory = fallbackFactory;
    }

    public Map<Method, Method> getFallbackMethodMap() {
        return fallbackMethodMap;
    }

    public void setFallbackMethodMap(Map<Method, Method> fallbackMethodMap) {
        this.fallbackMethodMap = fallbackMethodMap;
    }

    public Object getProxy() {
        return proxy;
    }

    public void setProxy(Object proxy) {
        this.proxy = proxy;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }

    public FeignException getFeignException() {
        return feignException;
    }

    public void setFeignException(FeignException feignException) {
        this.feignException = feignException;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
