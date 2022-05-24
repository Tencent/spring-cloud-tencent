# Spring Cloud Polaris Gray Release Example

English | [简体中文](./README-zh.md)

## Project Explanation

This project shows how to use Spring Cloud Tencent route and transitive feature to do the full chain gray releasing.

## Architecture

![](https://qcloudimg.tencent-cloud.cn/raw/488182fd3001b3e77d9450e2c8798ff3.png)

Incoming requests dispatched from Gateway service to 3 environments：
- gray1(match uid=1), env=green(green environment)
- gray2(match uid=2), env=purple(purple environment)
- baseline（stable environment, match all other requests）, env=blue(blue environment)

## How to access

### Start Gateway service

1. add environment variables

   - polaris server address: polaris_address=grpc://127.0.0.1:8091
   - pushgateway address: prometheus_address=127.0.0.1:9091

2. start router-grayrelease-gateway application

    - Launch by IDE：Start the main class `GrayReleaseGatewayApplication`.
    - Launch by Jar：Execute `mvn clean package` to compile with jar package, then use `java -jar router-grayrelease-gateway-${verion}.jar` to launch application.

3. add the route rule

    Send http request to polaris server to add the route rule, make requests dispatched to 3 environments.
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
   
   The route rule can be added by polaris console: 
   
   ![](https://qcloudimg.tencent-cloud.cn/raw/28e3d734c4b73624869a5b9b7059b118.png)
   
### Start Front service

#### Start baseline environment (blue)
   
1. add environment variables

   - polaris server address: polaris_address=grpc://127.0.0.1:8091
   - pushgateway address: prometheus_address=127.0.0.1:9091
   - env tag：SCT_METADATA_CONTENT_env=blue
   - transitive tag：SCT_METADATA_CONTENT_TRANSITIVE=env

2. start router-grayrelease-frontend application

    - Launch by IDE：Start the main class `GrayReleaseFrontApplication`.
    - Launch by Jar：Execute `mvn clean package` to compile with jar package, then use `java -jar router-grayrelease-frontend-${verion}.jar` to launch application.

#### Start gray1 environment (green)

1. add environment variables

   - polaris server address: polaris_address=grpc://127.0.0.1:8091
   - pushgateway address: prometheus_address=127.0.0.1:9091
   - env tag：SCT_METADATA_CONTENT_env=green
   - transitive tag：SCT_METADATA_CONTENT_TRANSITIVE=env

2. start router-grayrelease-frontend application (same as previous instruction) 
    
    If port conflicted, you can specify another port by -Dserver.port

#### Start gray2 environment (purple)

1. add environment variables

   - polaris server address: polaris_address=grpc://127.0.0.1:8091
   - pushgateway address: prometheus_address=127.0.0.1:9091
   - env tag：SCT_METADATA_CONTENT_env=purple
   - transitive tag：SCT_METADATA_CONTENT_TRANSITIVE=env

2. start router-grayrelease-frontend application (same as previous instruction) 

#### Start effective

You can find the instances with different tags in polaris console.

![](https://qcloudimg.tencent-cloud.cn/raw/96d2bdd2fb3495f737ab278e31a4a2e7.png)

### Start Middle service

#### Start baseline environment (blue)
   
1. add environment variables

   - polaris server address: polaris_address=grpc://127.0.0.1:8091
   - pushgateway address: prometheus_address=127.0.0.1:9091
   - env tag：SCT_METADATA_CONTENT_env=blue
   - transitive tag：SCT_METADATA_CONTENT_TRANSITIVE=env
   
2. start router-grayrelease-middle application

    - Launch by IDE：Start the main class `GrayReleaseMiddleApplication`.
    - Launch by Jar：Execute `mvn clean package` to compile with jar package, then use `java -jar router-grayrelease-middle-${verion}.jar` to launch application.

#### Start gray2 environment (purple)

1. add environment variables

   - polaris server address: polaris_address=grpc://127.0.0.1:8091
   - pushgateway address: prometheus_address=127.0.0.1:9091
   - env tag：SCT_METADATA_CONTENT_env=purple
   - transitive tag：SCT_METADATA_CONTENT_TRANSITIVE=env
   
2. start router-grayrelease-middle application (same as previous instruction) 

### Start Back service

#### Start baseline environment (blue)
   
1. add environment variables

   - polaris server address: polaris_address=grpc://127.0.0.1:8091
   - pushgateway address: prometheus_address=127.0.0.1:9091
   - env tag：SCT_METADATA_CONTENT_env=blue
   - transitive tag：SCT_METADATA_CONTENT_TRANSITIVE=env

2. start router-grayrelease-backend application

    - Launch by IDE：Start the main class `GrayReleaseBackendApplication`.
    - Launch by Jar：Execute `mvn clean package` to compile with jar package, then use `java -jar router-grayrelease-backend-${verion}.jar` to launch application.

#### Start gray1 environment (green)

1. add environment variables

   - polaris server address: polaris_address=grpc://127.0.0.1:8091
   - pushgateway address: prometheus_address=127.0.0.1:9091
   - env tag：SCT_METADATA_CONTENT_env=green
   - transitive tag：SCT_METADATA_CONTENT_TRANSITIVE=env
   
2. start router-grayrelease-backend application (same as previous instruction) 

### Test

#### Baseline routing

````
curl -H'uid:0' 127.0.0.1:59100/router/gray/route_rule
````
Got result
````
gray-release-gateway -> gray-release-front[blue] -> gray-release-middle[blue] -> gray-release-back[blue]
````

#### Green routing

````
curl -H'uid:1' 127.0.0.1:59100/router/gray/route_rule
````
Got result
````
gray-release-gateway -> gray-release-front[green] -> gray-release-middle[blue] -> gray-release-back[green]
````

#### Purple routing

````
curl -H'uid:2' 127.0.0.1:59100/router/gray/route_rule
````
Got result
````
gray-release-gateway -> gray-release-front[purple] -> gray-release-middle[purple] -> gray-release-back[blue]
````

