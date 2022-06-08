# Spring Cloud Polaris Gray Release Example

[English](./README.md) | 简体中文 

## 项目说明

本项目演示如何使用 Spring Cloud Tencent 的路由和标签透传功能 完成 Spring Cloud 应用的全链路灰度。

## 示例架构

![](https://qcloudimg.tencent-cloud.cn/raw/488182fd3001b3e77d9450e2c8798ff3.png)

本示例请求都通过最上层网关进行分发，分发的目的地主要涉及3个环境：
- 灰度环境1（只针对uid=1的请求放开），环境标识为env=green（绿色环境）
- 灰度环境2（只针对uid=2的请求放开），环境标识为env=purple（紫色环境）
- 基线环境（稳定的业务版本，针对其他请求放开），环境标识为env=blue（蓝色环境）

## 如何接入

### 启动网关服务

1. 添加环境变量

   - 北极星服务端地址：polaris_address=grpc://127.0.0.1:8091
   - 可观测性PushGateway地址：prometheus_address=127.0.0.1:9091

2. 启动router-grayrelease-gateway应用

    - IDE直接启动：找到主类 `GrayReleaseGatewayApplication`，执行 main 方法启动应用。
    - 打包编译后启动：首先执行 `mvn clean package` 将工程编译打包，然后执行 `java -jar router-grayrelease-gateway-${verion}.jar`启动应用。

3. 添加路由规则

    通过往北极星接口发送以下数据，为网关服务添加路由规则，路由规则可以针对用户ID进行环境的分发。
   ````
   POST /naming/v1/routings
   
   [{
   	"service": "gray-release-gateway",
   	"namespace": "default",
   	"outbounds": [
      {
   	    "sources": [
        {
   		   "service": "gray-release-gateway",
   		   "namespace": "default",
   		   "metadata": {
   			  "${http.header.uid}": {
   				 "type": "EXACT",
   			     "value": "2"
   			  }
   		   }
   	    }],
   	    "destinations": [
        {
   		   "service": "*",
   		   "namespace": "*",
   		   "metadata": {
   			  "env": {
   			    "type": "EXACT",
   			    "value": "purple"
   			  }
   		   },
   		   "priority": 0,
   		   "weight": 100,
   		   "isolate": false
   	    }]
   	  },
   	  {
   		"sources": [
        {
   		   "service": "gray-release-gateway",
   		   "namespace": "default",
   		   "metadata": {
   			 "${http.header.uid}": {
   				"type": "EXACT",
   				"value": "1"
   		     }
   		   }
   		}],
   		"destinations": [
        {
   			"service": "*",
   			"namespace": "*",
   			"metadata": {
   				"env": {
   					"type": "EXACT",
   					"value": "green"
   				}
   			},
   			"priority": 0,
   			"weight": 100,
   			"isolate": false
   		}]
   	  },
   	  {
   		"sources": [
        {
   			"service": "gray-release-gateway",
   			"namespace": "default",
   			"metadata": {
   				"*": {
   					"type": "EXACT",
   					"value": "*"
   				}
   			}
   		}],
   		"destinations": [
        {
   			"service": "*",
   			"namespace": "*",
   			"metadata": {
   				"env": {
   					"type": "EXACT",
   					"value": "blue"
   				}
   			},
   			"priority": 0,
   			"weight": 100,
   			"isolate": false
   		}]
   	  }
   	]
   }]
   ````
   
   路由规则也可以通过北极星控制台进行定义，最终控制台效果如下：
   
   ![](https://qcloudimg.tencent-cloud.cn/raw/28e3d734c4b73624869a5b9b7059b118.png)
   
### 启动Front服务

#### 启动基线环境（蓝色）
   
1. 添加环境变量

   - 北极星服务端地址：polaris_address=grpc://127.0.0.1:8091
   - 可观测性PushGateway地址：prometheus_address=127.0.0.1:9091
   - 环境标识：SCT_METADATA_CONTENT_env=blue
   - 透传环境标识：SCT_METADATA_CONTENT_TRANSITIVE=env

2. 启动router-grayrelease-frontend应用

    - IDE直接启动：找到主类 `GrayReleaseFrontApplication`，执行 main 方法启动应用。
    - 打包编译后启动：首先执行 `mvn clean package` 将工程编译打包，然后执行 `java -jar router-grayrelease-frontend-${verion}.jar`启动应用。

#### 启动灰度环境1（绿色）

1. 添加环境变量

   - 北极星服务端地址：polaris_address=grpc://127.0.0.1:8091
   - 可观测性PushGateway地址：prometheus_address=127.0.0.1:9091
   - 环境标识：SCT_METADATA_CONTENT_env=green
   - 透传环境标识：SCT_METADATA_CONTENT_TRANSITIVE=env
   
2. 启动router-grayrelease-frontend应用（与前面一致）
   
   如果遇到端口冲突，可以通过-Dserver.port来指定端口

#### 启动灰度环境2（紫色）

1. 添加环境变量

   - 北极星服务端地址：polaris_address=grpc://127.0.0.1:8091
   - 可观测性PushGateway地址：prometheus_address=127.0.0.1:9091
   - 环境标识：SCT_METADATA_CONTENT_env=purple
   - 透传环境标识：SCT_METADATA_CONTENT_TRANSITIVE=env
   
2. 启动router-grayrelease-frontend应用（与前面一致）

#### 启动后效果

在北极星控制台，可以看到gray-release-front服务下有3个节点，每个节点有不同的环境标识。

![](https://qcloudimg.tencent-cloud.cn/raw/96d2bdd2fb3495f737ab278e31a4a2e7.png)

### 启动middle服务

#### 启动基线环境（蓝色）

1. 添加环境变量

   - 北极星服务端地址：polaris_address=grpc://127.0.0.1:8091
   - 可观测性PushGateway地址：prometheus_address=127.0.0.1:9091
   - 环境标识：SCT_METADATA_CONTENT_env=blue
   - 透传环境标识：SCT_METADATA_CONTENT_TRANSITIVE=env

2. 启动router-grayrelease-middle应用

    - IDE直接启动：找到主类 `GrayReleaseMiddleApplication`，执行 main 方法启动应用。
    - 打包编译后启动：首先执行 `mvn clean package` 将工程编译打包，然后执行 `java -jar router-grayrelease-middle-${verion}.jar`启动应用。


#### 启动灰度环境2（紫色）

1. 添加环境变量

   - 北极星服务端地址：polaris_address=grpc://127.0.0.1:8091
   - 可观测性PushGateway地址：prometheus_address=127.0.0.1:9091
   - 环境标识：SCT_METADATA_CONTENT_env=purple
   - 透传环境标识：SCT_METADATA_CONTENT_TRANSITIVE=env
   
2. 启动router-grayrelease-middle应用（与前面一致）

### 启动back服务

#### 启动基线环境（蓝色）

1. 添加环境变量

   - 北极星服务端地址：polaris_address=grpc://127.0.0.1:8091
   - 可观测性PushGateway地址：prometheus_address=127.0.0.1:9091
   - 环境标识：SCT_METADATA_CONTENT_env=blue
   - 透传环境标识：SCT_METADATA_CONTENT_TRANSITIVE=env
   
2. 启动router-grayrelease-backend应用

    - IDE直接启动：找到主类 `GrayReleaseBackendApplication`，执行 main 方法启动应用。
    - 打包编译后启动：首先执行 `mvn clean package` 将工程编译打包，然后执行 `java -jar router-grayrelease-backend-${verion}.jar`启动应用。

#### 启动灰度环境1（绿色）

1. 添加环境变量

   - 北极星服务端地址：polaris_address=grpc://127.0.0.1:8091
   - 可观测性PushGateway地址：prometheus_address=127.0.0.1:9091
   - 环境标识：SCT_METADATA_CONTENT_env=green
   - 透传环境标识：SCT_METADATA_CONTENT_TRANSITIVE=env
   
2. 启动router-grayrelease-backend应用（与前面一致）

### 测试

#### 基线环境路由

````
curl -H'uid:0' 127.0.0.1:59100/router/gray/route_rule
````
获取结果
````
gray-release-gateway -> gray-release-front[blue] -> gray-release-middle[blue] -> gray-release-back[blue]
````

#### 灰度环境1（绿色）路由

````
curl -H'uid:1' 127.0.0.1:59100/router/gray/route_rule
````
获取结果
````
gray-release-gateway -> gray-release-front[green] -> gray-release-middle[blue] -> gray-release-back[green]
````

#### 灰度环境2（紫色）路由

````
curl -H'uid:2' 127.0.0.1:59100/router/gray/route_rule
````
获取结果
````
gray-release-gateway -> gray-release-front[purple] -> gray-release-middle[purple] -> gray-release-back[blue]
````

