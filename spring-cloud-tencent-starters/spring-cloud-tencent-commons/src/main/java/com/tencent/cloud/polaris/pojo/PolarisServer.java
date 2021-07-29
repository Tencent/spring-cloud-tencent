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

package com.tencent.cloud.polaris.pojo;

import com.google.common.base.Objects;
import com.netflix.loadbalancer.Server;
import com.tencent.polaris.api.pojo.Instance;
import com.tencent.polaris.api.pojo.ServiceInstances;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

/**
 * Polaris implementation of {@link Server}
 *
 * @author Haotian Zhang
 */
public class PolarisServer extends Server {

    private final ServiceInstances serviceInstances;

    private final Instance instance;

    private final MetaInfo metaInfo;

    public PolarisServer(ServiceInstances serviceInstances, Instance instance) {
        super(instance.getHost(), instance.getPort());
        if (StringUtils.equalsIgnoreCase(instance.getProtocol(), "https")) {
            setSchemea("https");
        } else {
            setSchemea("http");
        }
        this.serviceInstances = serviceInstances;
        this.instance = instance;
        this.metaInfo = new MetaInfo() {
            @Override
            public String getAppName() {
                return instance.getService();
            }

            @Override
            public String getServerGroup() {
                return null;
            }

            @Override
            public String getServiceIdForDiscovery() {
                return instance.getService();
            }

            @Override
            public String getInstanceId() {
                return instance.getId();
            }
        };
    }

    public Instance getInstance() {
        return instance;
    }

    @Override
    public MetaInfo getMetaInfo() {
        return metaInfo;
    }

    public Map<String, String> getMetadata() {
        return instance.getMetadata();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        PolarisServer that = (PolarisServer) o;
        return Objects.equal(instance, that.instance);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(super.hashCode(), instance);
    }

    public ServiceInstances getServiceInstances() {
        return serviceInstances;
    }
}
