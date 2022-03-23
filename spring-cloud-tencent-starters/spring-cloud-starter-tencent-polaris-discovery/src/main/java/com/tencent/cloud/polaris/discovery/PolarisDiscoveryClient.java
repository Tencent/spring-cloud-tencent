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

package com.tencent.cloud.polaris.discovery;

import java.util.List;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;

/**
 * Discovery Client for Polaris.
 *
 * @author Haotian Zhang, Andrew Shan, Jie Cheng
 */
public class PolarisDiscoveryClient implements DiscoveryClient {

    /**
     * Polaris Discovery Client Description.
     */
    public final String description = "Spring Cloud Polaris Discovery Client";

    private final PolarisServiceDiscovery polarisServiceDiscovery;

    public PolarisDiscoveryClient(PolarisServiceDiscovery polarisServiceDiscovery) {
        this.polarisServiceDiscovery = polarisServiceDiscovery;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public List<ServiceInstance> getInstances(String service) {
        return polarisServiceDiscovery.getInstances(service);
    }

    @Override
    public List<String> getServices() {
        return polarisServiceDiscovery.getServices();
    }

}
