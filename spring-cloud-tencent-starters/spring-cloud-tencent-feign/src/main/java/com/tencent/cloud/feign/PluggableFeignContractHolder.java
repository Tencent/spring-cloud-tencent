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

import feign.Contract;
import feign.MethodMetadata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contract for PluggableFeign
 *
 * @author Haotian Zhang
 */
public class PluggableFeignContractHolder implements Contract {

    private final Contract delegate;

    /**
     * Key of metadata is full name of method including full name of class, name of method and types of parameters.
     */
    public static final Map<String, MethodMetadata> METHOD_METADATA = new HashMap<>();

    public PluggableFeignContractHolder(Contract delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<MethodMetadata> parseAndValidatateMetadata(Class<?> targetType) {
        List<MethodMetadata> metadataList = delegate.parseAndValidatateMetadata(targetType);
        metadataList.forEach(metadata ->
                METHOD_METADATA.put(targetType.getPackage().getName() + "." + metadata.configKey(), metadata));
        return metadataList;
    }

}
