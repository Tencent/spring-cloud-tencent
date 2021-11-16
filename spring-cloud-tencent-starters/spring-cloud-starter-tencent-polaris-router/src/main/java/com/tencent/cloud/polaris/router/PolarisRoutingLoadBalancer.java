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

package com.tencent.cloud.polaris.router;

import com.tencent.cloud.metadata.constant.MetadataConstant.SystemMetadataKey;
import com.tencent.cloud.metadata.context.MetadataContextHolder;
import com.tencent.cloud.polaris.pojo.PolarisServiceInstance;
import com.tencent.polaris.api.pojo.DefaultInstance;
import com.tencent.polaris.api.pojo.DefaultServiceInstances;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInfo;
import com.tencent.polaris.api.pojo.ServiceInstances;
import com.tencent.polaris.api.pojo.ServiceKey;
import com.tencent.polaris.router.api.core.RouterAPI;
import com.tencent.polaris.router.api.rpc.ProcessRoutersRequest;
import com.tencent.polaris.router.api.rpc.ProcessRoutersResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.util.CollectionUtils;

/**
 * @author Haotian Zhang
 */
public class PolarisRoutingLoadBalancer {

    private final RouterAPI routerAPI;

    public PolarisRoutingLoadBalancer(RouterAPI routerAPI) {
        this.routerAPI = routerAPI;
    }

    public List<ServiceInstance> chooseInstances(List<ServiceInstance> allServers) {
        if (CollectionUtils.isEmpty(allServers)) {
            return allServers;
        }
        ServiceInstances serviceInstances = null;
        String serviceName = allServers.get(0).getServiceId();
        if (StringUtils.isBlank(serviceName)) {
            throw new IllegalStateException(
                    "PolarisRoutingLoadBalancer only Server with AppName or ServiceIdForDiscovery attribute");
        }
        ServiceKey serviceKey = new ServiceKey(MetadataContextHolder.LOCAL_NAMESPACE, serviceName);
        List<Instance> instances = new ArrayList<>(8);
        for (ServiceInstance server : allServers) {
            DefaultInstance instance = new DefaultInstance();
            instance.setNamespace(MetadataContextHolder.LOCAL_NAMESPACE);
            instance.setService(serviceName);
            instance.setProtocol(server.getScheme());
            instance.setId(server.getInstanceId());
            instance.setHost(server.getHost());
            instance.setPort(server.getPort());
            instance.setWeight(100);
            instances.add(instance);
        }
        serviceInstances = new DefaultServiceInstances(serviceKey, instances);
        ProcessRoutersRequest processRoutersRequest = new ProcessRoutersRequest();
        processRoutersRequest.setDstInstances(serviceInstances);
        String srcNamespace = MetadataContextHolder.get().getSystemMetadata(SystemMetadataKey.LOCAL_NAMESPACE);
        String srcService = MetadataContextHolder.get().getSystemMetadata(SystemMetadataKey.LOCAL_SERVICE);
        Map<String, String> transitiveCustomMetadata = MetadataContextHolder.get().getAllTransitiveCustomMetadata();
        String method = MetadataContextHolder.get().getSystemMetadata(SystemMetadataKey.PEER_PATH);
        processRoutersRequest.setMethod(method);
        if (StringUtils.isNotBlank(srcNamespace) && StringUtils.isNotBlank(srcService)) {
            ServiceInfo serviceInfo = new ServiceInfo();
            serviceInfo.setNamespace(srcNamespace);
            serviceInfo.setService(srcService);
            serviceInfo.setMetadata(transitiveCustomMetadata);
            processRoutersRequest.setSourceService(serviceInfo);
        }
        ProcessRoutersResponse processRoutersResponse = routerAPI.processRouters(processRoutersRequest);
        ServiceInstances filteredServiceInstances = processRoutersResponse.getServiceInstances();
        List<ServiceInstance> filteredInstances = new ArrayList<>();
        for (Instance instance : filteredServiceInstances.getInstances()) {
            filteredInstances.add(new PolarisServiceInstance(instance));
        }
        return filteredInstances;
    }

}
