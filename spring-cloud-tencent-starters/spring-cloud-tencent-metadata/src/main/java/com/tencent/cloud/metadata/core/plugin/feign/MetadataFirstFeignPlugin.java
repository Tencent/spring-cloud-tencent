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

package com.tencent.cloud.metadata.core.plugin.feign;

import com.tencent.cloud.feign.PluggableFeignContext;
import com.tencent.cloud.feign.PluggableFeignContractHolder;
import com.tencent.cloud.feign.PluggableFeignPlugin;
import com.tencent.cloud.feign.PluggableFeignPluginType;
import com.tencent.cloud.metadata.constant.MetadataConstant;
import com.tencent.cloud.metadata.context.MetadataContext;
import com.tencent.cloud.metadata.context.MetadataContextHolder;
import feign.Feign;
import feign.MethodMetadata;
import feign.RequestTemplate;

/**
 * Plugin used for adding the metadata in http headers from context when web client is Feign.
 *
 * @author Haotian Zhang
 */
public class MetadataFirstFeignPlugin implements PluggableFeignPlugin {

    @Override
    public int getOrder() {
        return MetadataConstant.OrderConstant.METADATA_FIRST_FEIGN_PLUGIN_ORDER;
    }

    @Override
    public String getName() {
        return MetadataFirstFeignPlugin.class.getName();
    }

    @Override
    public PluggableFeignPluginType getType() {
        return PluggableFeignPluginType.PRE;
    }

    @Override
    public void run(PluggableFeignContext context) {
        if (context.getTarget() != null && context.getMethod() != null) {
            MethodMetadata methodMetadata = PluggableFeignContractHolder.METHOD_METADATA
                    .get(context.getTarget().type().getPackage().getName() + "."
                            + Feign.configKey(context.getTarget().type(), context.getMethod()));
            if (methodMetadata == null) {
                return;
            }
            RequestTemplate requestTemplate = methodMetadata.template();
            // get metadata of current thread
            MetadataContext metadataContext = MetadataContextHolder.get();

            // TODO 对端命名空间暂时与本地命名空间相同
            MetadataContextHolder.get().putSystemMetadata(MetadataConstant.SystemMetadataKey.PEER_NAMESPACE,
                    metadataContext.getSystemMetadata(MetadataConstant.SystemMetadataKey.LOCAL_NAMESPACE));
            MetadataContextHolder.get().putSystemMetadata(MetadataConstant.SystemMetadataKey.PEER_SERVICE,
                    context.getTarget().name());
            MetadataContextHolder.get().putSystemMetadata(MetadataConstant.SystemMetadataKey.PEER_PATH,
                    requestTemplate.path());
        }
    }
}
