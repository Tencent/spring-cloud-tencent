## A Multi-Feature Environment Example

English | [简体中文](./README-zh.md)

## I. Deployment Structure

<img src="./imgs/structs.png" alt="multi-feature environment structure"/>

As shown in the figure above, there are three environments.
1. `baseline` environment, including `FrontService`, `MiddleService`, `BackendService`
2. `feature1` environment, including `MiddleService`, `BackendService`
3. `feature2` environment, including `FrontService`, `BackendService`

And at the entrance, deploy the `gateway` service.

Three request links.
1. `baseline` environment link, `Gateway` -> `FrontService`(baseline) -> `MiddleService`(baseline) -> `BackendService`(baseline)
2. `feature1` environment link, `Gateway` -> `FrontService`(baseline) -> `MiddleService`(feature1) -> `BackendService`(feature1)
3. `feature2` environment link, `Gateway` -> `FrontService`(feature2) -> `MiddleService`(baseline) -> `BackendService`(feature2)


## II. Running

Without any code changes, just start all the applications under `base`, `feature1`, `feature2`, `featureenv-gateway` directly.

By default, the applications point to the official Polaris experience environment, and you can directly view the service registration data at the experience site after a successful launch.

- Console address: http://14.116.241.63:8080/
  - Account：polaris
  - Password: polaris

## III. Testing

### Mode 1: Client Request With `featureenv` Label

#### `baseline` environment link
````
curl http://127.0.0.1:9999/featureenv-front-example/router/rest
````
Response results (base indicates baseline environment)
````
featureenv-front-example[base] -> featureenv-middle-example[base] -> featureenv-backend-example[base]
````

#### `feature1` environment link

Specify the feature environment via the `X-Polaris-Metadata-Transitive-featureenv` request header.

````
curl -H'X-Polaris-Metadata-Transitive-featureenv:feature1' http://127.0.0.1:9999/featureenv-front-example/router/rest
````
Response results
````
featureenv-front-example[base] -> featureenv-middle-example[feature1] -> featureenv-backend-example[feature1]
````

#### `feature2` environment link

Specify the feature environment via the `X-Polaris-Metadata-Transitive-featureenv` request header.

````
curl -H'X-Polaris-Metadata-Transitive-featureenv:feature2' http://127.0.0.1:9999/featureenv-front-example/router/rest
````
Response results
````
featureenv-front-example[feature2] -> featureenv-middle-example[base] -> featureenv-backend-example[feature2]
````

### Mode 2: Gateway traffic staining

Simulate a real-world scenario, assuming that the client request has a uid request parameter and expects:
1. `uid=1000` requests hit the `feature1` environment
2. `uid=2000` requests hit the `feature2` environment
3. requests with other uid hit the `baseline` environment

**Configure coloring rules**

Polaris Configuration Address：http://14.116.241.63:8080/#/filegroup-detail?group=featureenv-gateway&namespace=default

Modify the `rule/staining.json` configuration file and fill in the following rule:

````json
{
    "rules":[
        {
            "conditions":[
                {
                    "key":"${http.query.uid}",
                    "values":["1000"],
                    "operation":"EQUAL"
                }
            ],
            "labels":[
                {
                    "key":"featureenv",
                    "value":"feature1"
                }
            ]
        },
        {
            "conditions":[
                {
                    "key":"${http.query.uid}",
                    "values":["2000"],
                    "operation":"EQUAL"
                }
            ],
            "labels":[
                {
                    "key":"featureenv",
                    "value":"feature2"
                }
            ]
        }
    ]
}
````

Just fill out and publish the configuration.

#### `baseline` Environment Link
````
curl http://127.0.0.1:9999/featureenv-front-example/router/rest?uid=3000
````
Response results (base indicates baseline environment)
````
featureenv-front-example[base] -> featureenv-middle-example[base] -> featureenv-backend-example[base]
````

#### `feature1` Environment Link

Specify the feature environment via the `X-Polaris-Metadata-Transitive-featureenv` request header.

````
curl http://127.0.0.1:9999/featureenv-front-example/router/rest?uid=1000
````
Response results
````
featureenv-front-example[base] -> featureenv-middle-example[feature1] -> featureenv-backend-example[feature1]
````

#### `feature2` Environment Link

Specify the feature environment via the `X-Polaris-Metadata-Transitive-featureenv` request header.

````
curl http://127.0.0.1:9999/featureenv-front-example/router/rest?uid=2000
````
Response results
````
featureenv-front-example[feature2] -> featureenv-middle-example[base] -> featureenv-backend-example[feature2]
````


