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
package com.tencent.cloud.polaris.gateway.core.zuul.discovery;

import com.tencent.cloud.metadata.context.MetadataContextHolder;
import com.tencent.polaris.api.core.ConsumerAPI;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.rpc.GetAllInstancesRequest;
import com.tencent.polaris.api.rpc.InstancesResponse;
import com.tencent.polaris.factory.api.DiscoveryAPIFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: hongyihui
 */
public class PolarisProviderDiscovery {

    private static final Logger LOG = LoggerFactory.getLogger(PolarisProviderDiscovery.class);
    private static final Map<String, List<Instance>> polarisProviderMap = new ConcurrentHashMap<>();
    private static final Timer getAllServiceProviderTimer = new Timer("PolarisProviderDiscovery.getAllServiceProviderTimer", true);

    private ZuulProperties zuulProperties;

    public PolarisProviderDiscovery(ZuulProperties zuulProperties) {
        this.zuulProperties = zuulProperties;

        getAllServiceProvider();

        getAllServiceProviderTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                getAllServiceProvider();
            }
        }, 10000, 30000);
    }

    private void getAllServiceProvider() {
        Map<String, ZuulProperties.ZuulRoute> zuulRouteMap = zuulProperties.getRoutes();
        if (zuulRouteMap != null && zuulRouteMap.size() > 0) {
            ConsumerAPI consumerAPI = DiscoveryAPIFactory.createConsumerAPI();
            for (Map.Entry<String, ZuulProperties.ZuulRoute> entry : zuulRouteMap.entrySet()) {
                ZuulProperties.ZuulRoute zuulRoute = entry.getValue();
                String serviceId = zuulRoute.getServiceId();

                GetAllInstancesRequest getAllInstancesRequest = new GetAllInstancesRequest();
                getAllInstancesRequest.setNamespace(MetadataContextHolder.LOCAL_NAMESPACE);
                getAllInstancesRequest.setService(serviceId);
                InstancesResponse instancesResponse = consumerAPI.getAllInstance(getAllInstancesRequest);
                for (Instance instance : instancesResponse.getInstances()) {
                    LOG.info("instance is " + instance.getHost() + ":" + instance.getPort());
                }
                polarisProviderMap.put(MetadataContextHolder.LOCAL_NAMESPACE + "." + serviceId, Arrays.asList(instancesResponse.getInstances()));
            }
            consumerAPI.destroy();
        }
    }

    public Instance selectProvider(String namespace, String serviceId) {
        if (polarisProviderMap.size() == 0) {
            return null;
        }
        List<Instance> instances = polarisProviderMap.get(namespace + "." + serviceId);
        if (instances == null || instances.size() == 0) {
            return null;
        }

        // random select
        Random random = new Random();
        int selectIndex = random.nextInt(instances.size());
        return instances.get(selectIndex);
    }
}