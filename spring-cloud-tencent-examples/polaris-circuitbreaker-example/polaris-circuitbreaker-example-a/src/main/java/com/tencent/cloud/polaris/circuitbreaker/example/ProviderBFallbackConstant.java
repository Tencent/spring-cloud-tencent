package com.tencent.cloud.polaris.circuitbreaker.example;

public class ProviderBFallbackConstant {

	public static final String SERVICE_NAME = "polaris-circuitbreaker-example-b";

	public static final String API = "/example/service/b/info";

	public static final String FALLBACK_MESSAGE = "trigger the refuse for service b";
}
